package org.gca.trama8583.escenarios;

import org.gca.trama8583.nucleo.DefinicionCampo;
import org.gca.trama8583.nucleo.MensajeIso;
import org.gca.trama8583.nucleo.PerfilMensaje;
import org.gca.trama8583.nucleo.TipoDato;

/** Datos sintéticos comunes. Cada llamada crea datos propios, sin estado compartido. */
final class DatosLaboratorio {
    private DatosLaboratorio() { }

    static PerfilMensaje.Constructor perfil() {
        return PerfilMensaje.crear("escenarios-laboratorio-v1")
                .campo(DefinicionCampo.fijo(3, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(39, TipoDato.TEXTO_ASCII, 2))
                .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8))
                .obligatorios("0200", 3, 11, 41)
                .obligatorios("0210", 3, 11, 39, 41);
    }

    static MensajeIso solicitud() {
        // Inserción fuera de orden para demostrar el orden ascendente en la trama.
        return MensajeIso.crear("0200").campo(41, "TERM0001")
                .campo(11, "000001").campo(3, "000000").construir();
    }

    static byte[] tramaSolicitud() {
        // MTI + bitmap hexadecimal (campos 3, 11 y 41) + contenidos.
        // Literal independiente: nunca se obtiene codificando el mensaje esperado.
        return Comprobaciones.ascii("0200" + "2020000000800000" + "000000" + "000001" + "TERM0001");
    }

    static byte[] tramaRespuesta() {
        return Comprobaciones.ascii("0210" + "2020000002800000" + "000000" + "000001" + "00" + "TERM0001");
    }
}
