package org.gca.trama8583.escenarios;

import java.util.List;
import org.gca.trama8583.nucleo.CodigoErrorIso;
import org.gca.trama8583.nucleo.ExcepcionIso;
import org.gca.trama8583.nucleo.MensajeIso;
import org.gca.trama8583.nucleo.PerfilMensaje;
import org.gca.trama8583.spring.CanalesIso;
import org.gca.trama8583.spring.ConfiguracionIntegracionIso;
import org.gca.trama8583.spring.PasarelaIso;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import static org.gca.trama8583.escenarios.Comprobaciones.*;
import static org.gca.trama8583.escenarios.DatosLaboratorio.*;

/** Contextos locales propios por escenario, siempre cerrados al terminar. */
final class EscenariosSpring {
    private EscenariosSpring() { }

    static List<Escenario> todos() {
        return List.of(
                new Escenario("SPR-01", "Usar la pasarela sin una aplicación Boot",
                        "Un contexto Spring local con un perfil y ConfiguracionIntegracionIso",
                        "La pasarela construye e interpreta una solicitud y una respuesta",
                        "Ambos mensajes coinciden con sus vectores independientes", EscenariosSpring::pasarela),
                new Escenario("SPR-02", "Conservar cabeceras de aplicación en los canales",
                        "Un mensaje Spring con una solicitud y la cabecera folioLocal",
                        "Se envía por codificación y luego por decodificación",
                        "La cabecera y el mensaje se conservan en ambos flujos", EscenariosSpring::cabeceras),
                new Escenario("SPR-03", "Continuar después de una entrada inválida",
                        "Una pasarela local lista para interpretar",
                        "Se envía una trama incompleta y luego una solicitud válida",
                        "La causa ISO conserva el diagnóstico y la siguiente llamada funciona", EscenariosSpring::recuperacion));
    }

    static AnnotationConfigApplicationContext contexto() {
        var contexto = new AnnotationConfigApplicationContext();
        try {
            contexto.registerBean(PerfilMensaje.class, () -> perfil().construir());
            contexto.register(ConfiguracionIntegracionIso.class);
            contexto.refresh();
            return contexto;
        } catch (RuntimeException | Error error) {
            contexto.close();
            throw error;
        }
    }

    static void pasarela() {
        try (var contexto = contexto()) {
            var pasarela = contexto.getBean(PasarelaIso.class);
            var respuesta = solicitud().copiar().mti("0210").campo(39, "00").construir();
            bytes(tramaSolicitud(), pasarela.construir(solicitud()), "La pasarela debe producir el vector de solicitud");
            mensaje(solicitud(), pasarela.interpretar(tramaSolicitud()));
            bytes(tramaRespuesta(), pasarela.construir(respuesta), "La pasarela debe producir el vector de respuesta");
            mensaje(respuesta, pasarela.interpretar(tramaRespuesta()));
        }
    }

    static void cabeceras() {
        try (var contexto = contexto()) {
            var plantilla = new MessagingTemplate();
            plantilla.setReceiveTimeout(1000);
            var entrada = MessageBuilder.withPayload(solicitud()).setHeader("folioLocal", "laboratorio-42").build();
            var codificado = plantilla.sendAndReceive(contexto.getBean(CanalesIso.CODIFICACION, MessageChannel.class), entrada);
            if (codificado == null) { throw new AssertionError("El canal debe responder al codificar"); }
            igual("laboratorio-42", codificado.getHeaders().get("folioLocal"), "Debe conservar la cabecera al codificar");
            bytes(tramaSolicitud(), (byte[]) codificado.getPayload(), "El canal debe producir los bytes conocidos");
            var recibido = plantilla.sendAndReceive(contexto.getBean(CanalesIso.DECODIFICACION, MessageChannel.class), codificado);
            if (recibido == null) { throw new AssertionError("El canal debe responder al decodificar"); }
            igual("laboratorio-42", recibido.getHeaders().get("folioLocal"), "Debe conservar la cabecera al interpretar");
            mensaje(solicitud(), (MensajeIso) recibido.getPayload());
        }
    }

    static void recuperacion() {
        try (var contexto = contexto()) {
            var pasarela = contexto.getBean(PasarelaIso.class);
            try {
                pasarela.interpretar(new byte[]{0});
                throw new AssertionError("Debe rechazar la trama incompleta");
            } catch (RuntimeException error) {
                Throwable causa = error;
                while (!(causa instanceof ExcepcionIso) && causa.getCause() != null) { causa = causa.getCause(); }
                if (!(causa instanceof ExcepcionIso iso)) { throw new AssertionError("Debe conservar la causa ISO", error); }
                diagnostico(iso, CodigoErrorIso.TRAMA_INCOMPLETA, null, 0);
            }
            bytes(tramaSolicitud(), pasarela.construir(solicitud()), "La pasarela debe seguir construyendo después del error");
            mensaje(solicitud(), pasarela.interpretar(tramaSolicitud()));
        }
    }
}
