package org.gca.trama8583.escenarios;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import org.gca.trama8583.nucleo.CodigoErrorIso;
import org.gca.trama8583.nucleo.ExcepcionIso;
import org.gca.trama8583.nucleo.MensajeIso;

/** Comprobaciones activas sin -ea y sin JUnit en ejecución. */
final class Comprobaciones {
    private Comprobaciones() { }

    static byte[] ascii(String texto) { return texto.getBytes(StandardCharsets.US_ASCII); }

    static byte[] concatenar(byte[]... segmentos) {
        var salida = new java.io.ByteArrayOutputStream();
        for (byte[] segmento : segmentos) { salida.writeBytes(segmento); }
        return salida.toByteArray();
    }

    static void igual(Object esperado, Object obtenido, String contrato) {
        if (!Objects.equals(esperado, obtenido)) { throw new AssertionError(contrato); }
    }

    static void bytes(byte[] esperado, byte[] obtenido, String contrato) {
        if (!Arrays.equals(esperado, obtenido)) { throw new AssertionError(contrato); }
    }

    static void mensaje(MensajeIso esperado, MensajeIso obtenido) {
        igual(esperado.mti(), obtenido.mti(), "Debe conservar el MTI");
        igual(esperado.campos(), obtenido.campos(), "Debe conservar todos los campos y sus valores");
        bytes(esperado.cabecera(), obtenido.cabecera(), "Debe conservar la cabecera");
    }

    static void error(CodigoErrorIso codigo, Integer campo, int posicion, Runnable operacion) {
        try {
            operacion.run();
        } catch (ExcepcionIso error) {
            diagnostico(error, codigo, campo, posicion);
            return;
        }
        throw new AssertionError("Debe rechazar la operación con " + codigo);
    }

    static void diagnostico(ExcepcionIso error, CodigoErrorIso codigo, Integer campo, int posicion) {
        igual(codigo, error.codigo(), "Debe conservar el código de error esperado: " + codigo);
        igual(campo, error.campo(), "Debe identificar el campo del error");
        igual(posicion, error.posicion(), "Debe identificar la posición del error");
    }
}
