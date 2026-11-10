package org.gca.trama8583.spring;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.gca.trama8583.nucleo.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import static org.junit.jupiter.api.Assertions.*;

class IntegracionIsoTest {
    @Configuration(proxyBeanMethods = false)
    @Import(ConfiguracionIntegracionIso.class)
    static class Laboratorio {
        @Bean PerfilMensaje perfil() {
            return PerfilMensaje.crear("laboratorio")
                    .campo(DefinicionCampo.fijo(3, TipoDato.NUMERICO_ASCII, 6))
                    .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                    .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8))
                    .obligatorios("0200", 3, 11, 41).construir();
        }
    }
    private MensajeIso solicitud(String folio) {
        return MensajeIso.crear("0200").campo(3, "000000").campo(11, folio).campo(41, "TERM0001").construir();
    }
    @Test void pasarelaProcesaTramaConocidaEnAmbosSentidos() {
        try (var contexto = new AnnotationConfigApplicationContext(Laboratorio.class)) {
            var pasarela = contexto.getBean(PasarelaIso.class);
            byte[] esperado = "02002020000000800000000000000001TERM0001".getBytes(StandardCharsets.US_ASCII);
            assertArrayEquals(esperado, pasarela.construir(solicitud("000001")));
            assertEquals(solicitud("000001").campos(), pasarela.interpretar(esperado).campos());
        }
    }
    @Test void canalesConservanCabecerasDeAplicacion() {
        try (var contexto = new AnnotationConfigApplicationContext(Laboratorio.class)) {
            var plantilla = new MessagingTemplate(); plantilla.setReceiveTimeout(1000);
            var canal = contexto.getBean(CanalesIso.CODIFICACION, MessageChannel.class);
            var respuesta = plantilla.sendAndReceive(canal,
                    MessageBuilder.withPayload(solicitud("000001")).setHeader("folioLocal", "prueba-1").build());
            assertNotNull(respuesta);
            assertInstanceOf(byte[].class, respuesta.getPayload());
            assertEquals("prueba-1", respuesta.getHeaders().get("folioLocal"));
            var interpretado = plantilla.sendAndReceive(contexto.getBean(CanalesIso.DECODIFICACION, MessageChannel.class), respuesta);
            assertInstanceOf(MensajeIso.class, interpretado.getPayload());
            assertEquals("prueba-1", interpretado.getHeaders().get("folioLocal"));
        }
    }
    @Test void propagaErrorDelNucleoConSuCodigo() {
        try (var contexto = new AnnotationConfigApplicationContext(Laboratorio.class)) {
            var pasarela = contexto.getBean(PasarelaIso.class);
            var error = assertThrows(RuntimeException.class, () -> pasarela.interpretar(new byte[]{0}));
            Throwable causa = error;
            while (causa.getCause() != null) { causa = causa.getCause(); }
            assertEquals(CodigoErrorIso.TRAMA_INCOMPLETA, assertInstanceOf(ExcepcionIso.class, causa).codigo());
        }
    }
    @Test void solicitudesConcurrentesNoCruzanRespuestas() throws Exception {
        try (var contexto = new AnnotationConfigApplicationContext(Laboratorio.class);
             var ejecutor = Executors.newFixedThreadPool(4)) {
            var pasarela = contexto.getBean(PasarelaIso.class);
            var tareas = new ArrayList<Callable<Void>>();
            for (int indice = 0; indice < 100; indice++) {
                String folio = String.format(Locale.ROOT, "%06d", indice);
                tareas.add(() -> {
                    assertEquals(folio, pasarela.interpretar(pasarela.construir(solicitud(folio))).texto(11));
                    return null;
                });
            }
            for (var resultado : ejecutor.invokeAll(tareas, 5, TimeUnit.SECONDS)) { resultado.get(); }
        }
    }
}
