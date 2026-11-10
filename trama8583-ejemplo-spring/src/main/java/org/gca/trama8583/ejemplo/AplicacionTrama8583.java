package org.gca.trama8583.ejemplo;

import org.gca.trama8583.nucleo.DefinicionCampo;
import org.gca.trama8583.nucleo.MensajeIso;
import org.gca.trama8583.nucleo.PerfilMensaje;
import org.gca.trama8583.nucleo.TipoDato;
import org.gca.trama8583.spring.ConfiguracionIntegracionIso;
import org.gca.trama8583.spring.PasarelaIso;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Ejemplo local de laboratorio. No conecta a una red ni autoriza operaciones. */
@SpringBootApplication
@Import(ConfiguracionIntegracionIso.class)
public class AplicacionTrama8583 {
    public static void main(String[] argumentos) {
        try (var contexto = SpringApplication.run(AplicacionTrama8583.class, argumentos)) {
            // La demostracion termina al completar el CommandLineRunner.
        }
    }

    @Bean
    public PerfilMensaje perfilLaboratorio() {
        return PerfilMensaje.crear("laboratorio-ascii-v1")
                .campo(DefinicionCampo.fijo(3, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(39, TipoDato.TEXTO_ASCII, 2))
                .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8))
                .obligatorios("0200", 3, 11, 41)
                .obligatorios("0210", 3, 11, 39, 41)
                .construir();
    }

    @Bean
    public CommandLineRunner demostrarMensajes(PasarelaIso pasarela) {
        return argumentos -> {
            MensajeIso solicitud = MensajeIso.crear("0200")
                    .campo(3, "000000").campo(11, "000001").campo(41, "TERM0001")
                    .construir();
            byte[] trama = pasarela.construir(solicitud);
            MensajeIso interpretado = pasarela.interpretar(trama);
            MensajeIso respuesta = interpretado.copiar().mti("0210").campo(39, "00").construir();
            byte[] tramaRespuesta = pasarela.construir(respuesta);
            System.out.println("Solicitud interpretada: " + interpretado);
            System.out.println("Respuesta sintetica construida: " + tramaRespuesta.length + " bytes");
        };
    }
}
