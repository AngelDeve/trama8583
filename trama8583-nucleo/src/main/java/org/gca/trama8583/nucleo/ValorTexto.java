package org.gca.trama8583.nucleo;

import java.util.Objects;

/** Texto que conserva ceros, signos y espacios originales. */
public record ValorTexto(String texto) implements ValorCampo {
    public ValorTexto { Objects.requireNonNull(texto, "El texto no puede ser nulo"); }
    @Override public String toString() { return "ValorTexto[contenido omitido]"; }
}
