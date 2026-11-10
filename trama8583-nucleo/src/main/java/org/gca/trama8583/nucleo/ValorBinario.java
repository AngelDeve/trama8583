package org.gca.trama8583.nucleo;

import java.util.Arrays;
import java.util.Objects;

/** Bytes opacos con copias defensivas al entrar y salir. */
public final class ValorBinario implements ValorCampo {
    private final byte[] bytes;
    public ValorBinario(byte[] bytes) {
        this.bytes = Objects.requireNonNull(bytes, "Los bytes no pueden ser nulos").clone();
    }
    public byte[] bytes() { return bytes.clone(); }
    public int longitud() { return bytes.length; }
    @Override public String toString() { return "ValorBinario[contenido omitido]"; }
    @Override public boolean equals(Object otro) {
        return otro instanceof ValorBinario valor && Arrays.equals(bytes, valor.bytes);
    }
    @Override public int hashCode() { return Arrays.hashCode(bytes); }
}
