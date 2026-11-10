package org.gca.trama8583.escenarios;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class EjecutarEstresTest {
    @TempDir Path temporal;

    @ParameterizedTest(name = "Carga {0} valida sus vectores y diagnósticos")
    @ValueSource(strings = {"basica", "especiales", "voluminosa", "rechazos", "mixta", "spring"})
    void cargasValidanResultadosConFoliosDistintos(String nombre) {
        try (var carga = new CargasEstres(nombre)) {
            for (int indice = 0; indice < 128; indice++) {
                boolean rechazo = carga.ejecutar(indice);
                assertEquals(nombre.equals("rechazos") || (nombre.equals("mixta") && indice % 4 == 0), rechazo);
            }
        }
    }

    @Test void conteosConcurrentesDistinguenRechazosEsperados() throws Exception {
        var resultado = EjecutarEstres.medir(4, 1, 120, secuencia -> secuencia % 4 == 0);
        assertEquals(480, resultado.operaciones());
        assertEquals(360, resultado.correctas());
        assertEquals(120, resultado.rechazos());
        assertEquals(0, resultado.errores());
        assertEquals(480, resultado.latencias().cantidad());
        assertTrue(resultado.duracionNs() > 0);
    }

    @Test void fallosDeContratoSeContabilizanYNoSeConfundenConRechazos() throws Exception {
        var resultado = EjecutarEstres.medir(2, 1, 10, secuencia -> { throw new AssertionError("Fallo de contrato"); });
        assertEquals(20, resultado.errores());
        assertEquals(0, resultado.correctas());
        assertEquals(0, resultado.rechazos());
        assertEquals("AssertionError", resultado.primerError());
        assertFalse(resultado.cumple(EjecutarEstres.Opciones.leer(new String[0])));
    }

    @Test void cliProduceAmbosReportesYFallaAnteUnUmbralIncumplido() throws Exception {
        Path destino = temporal.resolve("reporte.json");
        try (var salida = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
            int codigo = EjecutarEstres.ejecutar(new String[]{"--hilos", "2", "--calentamiento", "0",
                    "--operaciones-por-hilo", "100", "--carga", "mixta", "--min-ops", "1e100", "--salida", destino.toString()}, salida, salida);
            assertEquals(1, codigo);
        }
        String json = Files.readString(destino);
        assertTrue(json.contains("\"operaciones\": 200"));
        assertTrue(json.contains("\"erroresInesperados\": 0"));
        assertTrue(json.contains("\"cumple\": false"));
        assertTrue(json.contains("\"p99NsLimiteSuperior\":"));
        assertEquals(2, Files.readAllLines(temporal.resolve("reporte.csv")).size());
    }

    @Test void argumentosInvalidosSeRechazanAntesDeCrearReportes() {
        for (String[] argumentos : new String[][]{
                {"--hilos", "0"}, {"--hilos", "65"}, {"--carga", "inexistente"}, {"--min-ops", "NaN"},
                {"--max-p99-ms", "Infinity"}, {"--segundos", "0"}, {"--calentamiento", "-1"},
                {"--salida", "resultado.csv"}, {"--hilos"}, {"--hilos", "1", "--hilos", "2"}, {"--opcion", "1"}}) {
            assertThrows(IllegalArgumentException.class, () -> EjecutarEstres.Opciones.leer(argumentos));
        }
    }
}
