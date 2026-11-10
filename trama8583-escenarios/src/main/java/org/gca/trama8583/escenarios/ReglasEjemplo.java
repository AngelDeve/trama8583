package org.gca.trama8583.escenarios;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.gca.trama8583.nucleo.MensajeIso;

/** Reglas inventadas de aplicación, externas al codec. Reciben ambas instantáneas explícitamente. */
final class ReglasEjemplo {
    private ReglasEjemplo() { }

    static List<String> validar(MensajeIso solicitud, MensajeIso respuesta) {
        var errores = new ArrayList<String>();
        for (int campo : new int[]{11, 41}) {
            if (solicitud.campo(campo) == null || !Objects.equals(solicitud.campo(campo), respuesta.campo(campo))) {
                errores.add("ECO_" + campo);
            }
        }
        boolean consulta = Objects.equals(solicitud.campo(3), new org.gca.trama8583.nucleo.ValorTexto("310000"));
        boolean exito = Objects.equals(respuesta.campo(39), new org.gca.trama8583.nucleo.ValorTexto("00"));
        if (consulta && exito && respuesta.campo(54) == null) { errores.add("MONTO_REQUERIDO"); }
        if (consulta && !exito && respuesta.campo(54) != null) { errores.add("MONTO_PROHIBIDO"); }
        return List.copyOf(errores);
    }
}
