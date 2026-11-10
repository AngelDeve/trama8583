package org.gca.trama8583.escenarios;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistogramaLatenciaTest {
    @Test void percentilesSonLimitesSuperioresConPrecisionAcotada() {
        var histograma = new HistogramaLatencia();
        for (int valor = 1; valor <= 10000; valor++) { histograma.registrar(valor); }
        for (double fraccion : new double[]{.50, .95, .99}) {
            long exacto = (long) Math.ceil(10000 * fraccion);
            long estimado = histograma.percentil(fraccion);
            assertTrue(estimado >= exacto);
            assertTrue(estimado <= exacto * 1.03125 + 1);
        }
        assertEquals(10000, histograma.cantidad());
        assertEquals(1, histograma.minimo());
        assertEquals(10000, histograma.maximo());
        assertEquals(5000.5, histograma.media());
    }

    @Test void combinarConservaConteoYDistribucionSinRetenerMuestras() {
        var completo = new HistogramaLatencia();
        var primero = new HistogramaLatencia();
        var segundo = new HistogramaLatencia();
        for (int valor = 1; valor <= 10000; valor++) {
            completo.registrar(valor);
            (valor % 2 == 0 ? primero : segundo).registrar(valor);
        }
        primero.combinar(segundo);
        assertEquals(completo.cantidad(), primero.cantidad());
        assertEquals(completo.media(), primero.media());
        assertEquals(completo.percentil(.99), primero.percentil(.99));
        assertEquals(completo.minimo(), primero.minimo());
        assertEquals(completo.maximo(), primero.maximo());
    }

    @Test void fronterasVaciasYExtremasNoDesbordan() {
        var histograma = new HistogramaLatencia();
        assertEquals(0, histograma.percentil(.99));
        assertEquals(0, histograma.minimo());
        histograma.registrar(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, histograma.percentil(.99));
        assertEquals(Long.MAX_VALUE, histograma.maximo());
    }
}
