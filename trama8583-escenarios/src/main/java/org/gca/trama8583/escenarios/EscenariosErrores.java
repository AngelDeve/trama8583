package org.gca.trama8583.escenarios;

import java.util.Arrays;
import java.util.List;
import org.gca.trama8583.nucleo.CodecIsoBasico;
import org.gca.trama8583.nucleo.DefinicionCampo;
import org.gca.trama8583.nucleo.PerfilMensaje;
import org.gca.trama8583.nucleo.TipoDato;
import static org.gca.trama8583.nucleo.CodigoErrorIso.*;
import static org.gca.trama8583.escenarios.Comprobaciones.*;
import static org.gca.trama8583.escenarios.DatosLaboratorio.*;

/** Un rechazo esperado solo pasa si coinciden código, campo y posición. */
final class EscenariosErrores {
    private EscenariosErrores() { }

    static List<Escenario> todos() {
        return List.of(
                new Escenario("ERR-01", "Detectar un campo obligatorio ausente",
                        "Una solicitud 0200 sin terminal",
                        "Se intenta codificar con el campo 41 obligatorio",
                        "Se obtiene CAMPO_OBLIGATORIO_AUSENTE, campo 41, posición -1", EscenariosErrores::obligatorio),
                new Escenario("ERR-02", "Rechazar una longitud fija incompatible",
                        "Un perfil con terminal de ocho bytes",
                        "Se intenta codificar una terminal de nueve caracteres",
                        "Se obtiene LONGITUD_INVALIDA, campo 41, posición -1", EscenariosErrores::longitudFija),
                new Escenario("ERR-03", "Rechazar letras en un campo numérico",
                        "Un campo 3 numérico ASCII de seis bytes",
                        "Se intenta codificar 00A000",
                        "Se obtiene CODIFICACION_INVALIDA, campo 3, posición -1", EscenariosErrores::numerico),
                new Escenario("ERR-04", "Identificar el campo de una trama truncada",
                        "Un vector válido de 40 bytes al que se quita el último byte",
                        "Se interpreta la trama incompleta",
                        "Se obtiene TRAMA_INCOMPLETA, campo 41, posición 32", EscenariosErrores::truncamiento),
                new Escenario("ERR-05", "Detectar bytes después del último campo",
                        "Un vector válido de 40 bytes seguido de un byte adicional",
                        "Se interpreta la trama con contenido sobrante",
                        "Se obtiene DATOS_SOBRANTES, sin campo, posición 40", EscenariosErrores::sobrantes),
                new Escenario("ERR-06", "Requerir definiciones explícitas de campos",
                        "Un perfil sin definición del campo 48",
                        "Se intenta codificar una solicitud que incluye el campo 48",
                        "Se obtiene CAMPO_NO_DEFINIDO, campo 48, posición -1", EscenariosErrores::desconocido),
                new Escenario("ERR-07", "Validar una longitud LLVAR anunciada",
                        "Un campo 48 LLVAR con máximo de diez bytes",
                        "Se lee un prefijo que anuncia once bytes",
                        "Se obtiene LONGITUD_INVALIDA, campo 48, posición 20", EscenariosErrores::prefijoVariable),
                new Escenario("ERR-08", "Aplicar el límite total en ambos sentidos",
                        "Un perfil con límite de 39 bytes y una solicitud que requiere 40",
                        "Se intenta construir y leer la solicitud",
                        "Se obtiene LIMITE_EXCEDIDO al construir y al interpretar", EscenariosErrores::limite));
    }

    static void obligatorio() {
        var codec = new CodecIsoBasico(perfil().construir());
        var mensaje = solicitud().copiar().quitarCampo(41).construir();
        error(CAMPO_OBLIGATORIO_AUSENTE, 41, -1, () -> codec.codificar(mensaje));
    }

    static void longitudFija() {
        var codec = new CodecIsoBasico(perfil().construir());
        var mensaje = solicitud().copiar().campo(41, "TERM00010").construir();
        error(LONGITUD_INVALIDA, 41, -1, () -> codec.codificar(mensaje));
    }

    static void numerico() {
        var codec = new CodecIsoBasico(perfil().construir());
        var mensaje = solicitud().copiar().campo(3, "00A000").construir();
        error(CODIFICACION_INVALIDA, 3, -1, () -> codec.codificar(mensaje));
    }

    static void truncamiento() {
        var codec = new CodecIsoBasico(perfil().construir());
        byte[] incompleta = Arrays.copyOf(tramaSolicitud(), 39);
        error(TRAMA_INCOMPLETA, 41, 32, () -> codec.decodificar(incompleta));
    }

    static void sobrantes() {
        var codec = new CodecIsoBasico(perfil().construir());
        byte[] extendida = concatenar(tramaSolicitud(), new byte[]{0});
        error(DATOS_SOBRANTES, null, 40, () -> codec.decodificar(extendida));
    }

    static void desconocido() {
        var codec = new CodecIsoBasico(perfil().construir());
        var mensaje = solicitud().copiar().campo(48, "PEDIDO-42").construir();
        error(CAMPO_NO_DEFINIDO, 48, -1, () -> codec.codificar(mensaje));
    }

    static void prefijoVariable() {
        var perfil = PerfilMensaje.crear("llvar-limitado-v1")
                .campo(DefinicionCampo.llvar(48, TipoDato.TEXTO_ASCII, 10)).construir();
        var codec = new CodecIsoBasico(perfil);
        byte[] trama = ascii("0200" + "0000000000010000" + "11" + "PEDIDO-42");
        error(LONGITUD_INVALIDA, 48, 20, () -> codec.decodificar(trama));
    }

    static void limite() {
        var codec = new CodecIsoBasico(perfil().maximoBytes(39).construir());
        error(LIMITE_EXCEDIDO, 41, -1, () -> codec.codificar(solicitud()));
        error(LIMITE_EXCEDIDO, null, 0, () -> codec.decodificar(tramaSolicitud()));
    }
}
