package org.gca.trama8583.spring;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.gca.trama8583.nucleo.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de integracion con canales reales; sin servidor ni tiempos de espera artificiales. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanalesEscenariosTest {
    private AnnotationConfigApplicationContext contexto;
    private PasarelaIso pasarela;
    @Configuration(proxyBeanMethods = false)
    @Import(ConfiguracionIntegracionIso.class)
    static class Laboratorio {
        @Bean PerfilMensaje perfil() {
            return PerfilMensaje.crear("escenarios")
                    .campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 5))
                    .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                    .campo(DefinicionCampo.fijo(39, TipoDato.TEXTO_ASCII, 2))
                    .campo(DefinicionCampo.fijo(70, TipoDato.NUMERICO_ASCII, 3))
                    .obligatorios("0200", 11).obligatorios("0210", 11, 39).construir();
        }
        @Bean @Primary CodecObservado codecObservado(PerfilMensaje perfil) { return new CodecObservado(perfil); }
    }
    static class CodecObservado implements CodecIso {
        final AtomicReference<Thread> ultimoHilo = new AtomicReference<>();
        private final CodecIso delegado;
        CodecObservado(PerfilMensaje perfil) { delegado = new CodecIsoBasico(perfil); }
        @Override public byte[] codificar(MensajeIso mensaje) {
            ultimoHilo.set(Thread.currentThread()); return delegado.codificar(mensaje);
        }
        @Override public MensajeIso decodificar(byte[] trama) {
            ultimoHilo.set(Thread.currentThread()); return delegado.decodificar(trama);
        }
    }
    @BeforeAll void abrirContexto() {
        contexto = new AnnotationConfigApplicationContext(Laboratorio.class);
        pasarela = contexto.getBean(PasarelaIso.class);
    }
    @AfterAll void cerrarContexto() { if (contexto != null) { contexto.close(); } }
    private static byte[] ascii(String texto) { return texto.getBytes(StandardCharsets.US_ASCII); }
    private static Throwable causaFinal(Throwable error) {
        while (error.getCause() != null) { error = error.getCause(); }
        return error;
    }
    static Stream<Arguments> tramasInvalidas() {
        return Stream.of(
                Arguments.of("truncamiento", "0200", CodigoErrorIso.TRAMA_INCOMPLETA, null, 4),
                Arguments.of("MTI", "A2000000000000000000", CodigoErrorIso.MTI_INVALIDO, null, 0),
                Arguments.of("bitmap", "0200G000000000000000", CodigoErrorIso.BITMAP_INVALIDO, null, 4),
                Arguments.of("campo desconocido", "08001000000000000000", CodigoErrorIso.CAMPO_NO_DEFINIDO, 4, 20),
                Arguments.of("obligatorio", "02000000000000000000", CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, 11, -1),
                Arguments.of("longitud", "0800400000000000000006", CodigoErrorIso.LONGITUD_INVALIDA, 2, 20),
                Arguments.of("codificacion", "02000020000000000000000A01", CodigoErrorIso.CODIFICACION_INVALIDA, 11, 23),
                Arguments.of("sobrantes", "08000000000000000000X", CodigoErrorIso.DATOS_SOBRANTES, null, 20));
    }
    @ParameterizedTest(name = "pasarela propaga {0}") @MethodSource("tramasInvalidas")
    void pasarelaConservaCodigoCampoYPosicion(String escenario, String trama,
            CodigoErrorIso codigo, Integer campo, int posicion) {
        var error = assertThrows(RuntimeException.class, () -> pasarela.interpretar(ascii(trama)));
        var causa = assertInstanceOf(ExcepcionIso.class, causaFinal(error));
        assertAll(() -> assertEquals(codigo, causa.codigo()), () -> assertEquals(campo, causa.campo()),
                () -> assertEquals(posicion, causa.posicion()));
    }
    @Test void propagaValidacionAlConstruirYPermiteProcesarDespuesDelError() {
        var error = assertThrows(RuntimeException.class, () -> pasarela.construir(MensajeIso.crear("0210").campo(11, "000001").construir()));
        assertEquals(CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, assertInstanceOf(ExcepcionIso.class, causaFinal(error)).codigo());
        var solicitud = MensajeIso.crear("0200").campo(11, "000001").construir();
        byte[] esperado = ascii("02000020000000000000000001");
        assertArrayEquals(esperado, pasarela.construir(solicitud));
        assertEquals("000001", pasarela.interpretar(esperado).texto(11));
    }
    @Test void procesaRespuestaYSecundarioSinCambiarElMti() {
        var respuesta = MensajeIso.crear("0210").campo(11, "000001").campo(39, "00").construir();
        byte[] esperado = ascii("0210002000000200000000000100");
        assertArrayEquals(esperado, pasarela.construir(respuesta));
        assertEquals("0210", pasarela.interpretar(esperado).mti());
        byte[] secundario = ascii("080080000000000000000400000000000000301");
        assertArrayEquals(secundario, pasarela.construir(MensajeIso.crear("0800").campo(70, "301").construir()));
        assertEquals("301", pasarela.interpretar(secundario).texto(70));
    }
    @Test void canalesEjecutanCodecEnElHiloDelLlamador() {
        var observador = contexto.getBean(CodecObservado.class);
        Thread llamador = Thread.currentThread();
        byte[] trama = pasarela.construir(MensajeIso.crear("0800").construir());
        assertSame(llamador, observador.ultimoHilo.get());
        pasarela.interpretar(trama);
        assertSame(llamador, observador.ultimoHilo.get());
    }
    @ParameterizedTest @ValueSource(strings = {CanalesIso.CODIFICACION, CanalesIso.DECODIFICACION})
    void canalRechazaPayloadIncompatible(String nombre) {
        var canal = contexto.getBean(nombre, DirectChannel.class);
        assertThrows(MessagingException.class,
                () -> canal.send(MessageBuilder.withPayload(new Object()).build()));
    }
    @Test void mensajeSpringConservaCabecerasYElOriginal() {
        var plantilla = new MessagingTemplate(); plantilla.setReceiveTimeout(1000);
        var original = MensajeIso.crear("0200").campo(11, "000001").construir();
        var peticion = MessageBuilder.withPayload(original).setHeader("folioLocal", "escenario-1")
                .setHeader("perfilLocal", "escenarios").build();
        var codificado = plantilla.sendAndReceive(contexto.getBean(CanalesIso.CODIFICACION, DirectChannel.class), peticion);
        assertNotNull(codificado);
        var interpretado = plantilla.sendAndReceive(contexto.getBean(CanalesIso.DECODIFICACION, DirectChannel.class), codificado);
        assertNotNull(interpretado);
        assertAll(() -> assertEquals("escenario-1", interpretado.getHeaders().get("folioLocal")),
                () -> assertEquals("escenarios", interpretado.getHeaders().get("perfilLocal")),
                () -> assertSame(original, peticion.getPayload()),
                () -> assertEquals(original.campos(), ((MensajeIso) interpretado.getPayload()).campos()));
    }
    @Configuration(proxyBeanMethods = false)
    static class DosPerfiles {
        @Bean PerfilMensaje primero() { return PerfilMensaje.crear("primero").construir(); }
        @Bean PerfilMensaje segundo() { return PerfilMensaje.crear("segundo").construir(); }
    }
    @Test void configuracionRequierePerfilDelIntegrador() {
        try (var incompleto = new AnnotationConfigApplicationContext()) {
            incompleto.register(ConfiguracionIntegracionIso.class);
            var error = assertThrows(BeansException.class, incompleto::refresh);
            assertInstanceOf(NoSuchBeanDefinitionException.class, causaFinal(error));
        }
    }
    @Test void configuracionRechazaSeleccionAmbiguaDePerfil() {
        try (var ambiguo = new AnnotationConfigApplicationContext()) {
            ambiguo.register(ConfiguracionIntegracionIso.class, DosPerfiles.class);
            var error = assertThrows(BeansException.class, ambiguo::refresh);
            assertInstanceOf(NoUniqueBeanDefinitionException.class, causaFinal(error));
        }
    }
}
