package org.gca.trama8583.nucleo;

/** Prefijos de longitud ASCII, medidos en bytes del contenido. */
public enum TipoLongitud {
    FIJA(0, Integer.MAX_VALUE), LLVAR(2, 99), LLLVAR(3, 999);
    private final int digitosPrefijo;
    private final int capacidad;
    TipoLongitud(int digitosPrefijo, int capacidad) {
        this.digitosPrefijo = digitosPrefijo;
        this.capacidad = capacidad;
    }
    public int digitosPrefijo() { return digitosPrefijo; }
    public int capacidad() { return capacidad; }
}
