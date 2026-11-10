package org.gca.trama8583.escenarios;

/** Un caso de uso y su comprobación; se ejecuta igual desde consola y desde JUnit. */
public record Escenario(String id, String titulo, String dado, String cuando,
                        String entonces, Runnable verificar) {
    @Override public String toString() { return id + " — " + titulo; }
}
