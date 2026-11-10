package org.gca.trama8583.escenarios;

/** Histograma acotado: 32 subdivisiones por potencia de dos, sin retener una muestra por operación. */
final class HistogramaLatencia {
    private final long[] cubetas = new long[64 * 32];
    private long cantidad;
    private long minimo = Long.MAX_VALUE;
    private long maximo;
    private double suma;

    void registrar(long nanosegundos) {
        long valor = Math.max(1, nanosegundos);
        int exponente = 63 - Long.numberOfLeadingZeros(valor);
        long base = 1L << exponente;
        int fraccion = exponente >= 5 ? (int) ((valor - base) >>> (exponente - 5)) : (int) ((valor - base) * 32 / base);
        cubetas[exponente * 32 + fraccion]++;
        cantidad++;
        minimo = Math.min(minimo, valor);
        maximo = Math.max(maximo, valor);
        suma += valor;
    }

    void combinar(HistogramaLatencia otro) {
        for (int indice = 0; indice < cubetas.length; indice++) { cubetas[indice] += otro.cubetas[indice]; }
        cantidad += otro.cantidad;
        minimo = Math.min(minimo, otro.minimo);
        maximo = Math.max(maximo, otro.maximo);
        suma += otro.suma;
    }

    long percentil(double fraccion) {
        if (cantidad == 0) { return 0; }
        long objetivo = (long) Math.ceil(cantidad * fraccion);
        long acumulado = 0;
        for (int indice = 0; indice < cubetas.length; indice++) {
            acumulado += cubetas[indice];
            if (acumulado >= objetivo) {
                int exponente = indice / 32;
                long base = 1L << exponente;
                int subcubeta = indice % 32;
                long ancho = exponente >= 5 ? base / 32 : 1;
                long superior = exponente >= 5 ? ((Long.MAX_VALUE - base) / ancho < subcubeta + 1L
                        ? Long.MAX_VALUE : base + (subcubeta + 1L) * ancho - 1)
                        : base + ((subcubeta + 1L) * base + 31) / 32 - 1;
                return Math.min(superior, maximo);
            }
        }
        return maximo;
    }

    long cantidad() { return cantidad; }
    long minimo() { return cantidad == 0 ? 0 : minimo; }
    long maximo() { return maximo; }
    double media() { return cantidad == 0 ? 0 : suma / cantidad; }
}
