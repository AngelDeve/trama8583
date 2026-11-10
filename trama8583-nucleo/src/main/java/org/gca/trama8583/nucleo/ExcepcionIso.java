package org.gca.trama8583.nucleo;

/** Diagnostico estructural sin valores de los campos ni copia de la trama. */
public final class ExcepcionIso extends RuntimeException {
    private final CodigoErrorIso codigo;
    private final Integer campo;
    private final int posicion;

    public ExcepcionIso(CodigoErrorIso codigo, Integer campo, int posicion, String descripcion) {
        super(descripcion);
        this.codigo = codigo;
        this.campo = campo;
        this.posicion = posicion;
    }

    public CodigoErrorIso codigo() { return codigo; }
    public Integer campo() { return campo; }
    /** Posicion desde cero en la trama, o -1 si no existe una trama de entrada. */
    public int posicion() { return posicion; }
}
