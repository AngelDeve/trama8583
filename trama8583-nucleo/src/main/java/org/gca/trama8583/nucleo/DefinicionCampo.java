package org.gca.trama8583.nucleo;

import java.util.Objects;

/** El largo fijo o maximo variable se expresa en bytes, sin incluir prefijo. */
public record DefinicionCampo(int numero, TipoDato tipoDato, TipoLongitud tipoLongitud, int longitudMaxima) {
    public DefinicionCampo {
        MensajeIso.validarNumeroCampo(numero);
        Objects.requireNonNull(tipoDato, "El tipo de dato es obligatorio");
        Objects.requireNonNull(tipoLongitud, "El tipo de longitud es obligatorio");
        if (longitudMaxima < 1 || longitudMaxima > tipoLongitud.capacidad()) {
            throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, numero, -1,
                    "La longitud del campo no puede representarse con el formato elegido");
        }
    }
    public static DefinicionCampo fijo(int numero, TipoDato tipo, int longitud) {
        return new DefinicionCampo(numero, tipo, TipoLongitud.FIJA, longitud);
    }
    public static DefinicionCampo llvar(int numero, TipoDato tipo, int maximo) {
        return new DefinicionCampo(numero, tipo, TipoLongitud.LLVAR, maximo);
    }
    public static DefinicionCampo lllvar(int numero, TipoDato tipo, int maximo) {
        return new DefinicionCampo(numero, tipo, TipoLongitud.LLLVAR, maximo);
    }
}
