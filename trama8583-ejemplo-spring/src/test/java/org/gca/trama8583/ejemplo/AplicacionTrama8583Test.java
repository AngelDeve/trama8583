package org.gca.trama8583.ejemplo;

import java.util.Map;
import org.gca.trama8583.nucleo.MensajeIso;
import org.gca.trama8583.spring.PasarelaIso;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import static org.junit.jupiter.api.Assertions.*;

class AplicacionTrama8583Test {
    @Test void arrancaBootEjecutaDemostracionYProcesaRespuestaSintetica() {
        var aplicacion = new SpringApplication(AplicacionTrama8583.class);
        aplicacion.setDefaultProperties(Map.of("spring.main.banner-mode", "off", "logging.level.root", "ERROR"));
        try (var contexto = aplicacion.run()) {
            assertTrue(contexto.isActive());
            var pasarela = contexto.getBean(PasarelaIso.class);
            var respuesta = MensajeIso.crear("0210").campo(3, "000000").campo(11, "000001")
                    .campo(39, "00").campo(41, "TERM0001").construir();
            byte[] trama = pasarela.construir(respuesta);
            assertEquals(42, trama.length);
            assertEquals(respuesta.campos(), pasarela.interpretar(trama).campos());
        }
    }
}
