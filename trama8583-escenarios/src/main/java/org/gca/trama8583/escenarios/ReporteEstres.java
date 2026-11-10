package org.gca.trama8583.escenarios;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/** Reportes con métricas y contexto de JVM, sin tramas, campos ni variables de entorno. */
final class ReporteEstres {
    private ReporteEstres() { }

    record EstadoJvm(long heapUsadoBytes, long coleccionesGc, long tiempoGcMs) { }

    static EstadoJvm estadoJvm() {
        long colecciones = 0, tiempo = 0;
        for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            colecciones += Math.max(0, gc.getCollectionCount());
            tiempo += Math.max(0, gc.getCollectionTime());
        }
        return new EstadoJvm(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), colecciones, tiempo);
    }

    static void guardar(EjecutarEstres.Opciones opciones, EjecutarEstres.Resultado resultado,
                        EstadoJvm antes, EstadoJvm despues) throws IOException {
        var valores = new LinkedHashMap<String, Object>();
        valores.put("schemaVersion", 1);
        valores.put("fechaUtc", Instant.now().toString());
        valores.put("versionBiblioteca", "1.0-SNAPSHOT");
        valores.put("javaVersion", System.getProperty("java.version"));
        valores.put("jvm", System.getProperty("java.vm.name"));
        valores.put("sistema", System.getProperty("os.name"));
        valores.put("arquitectura", System.getProperty("os.arch"));
        valores.put("procesadoresDisponibles", Runtime.getRuntime().availableProcessors());
        valores.put("heapMaximoBytes", Runtime.getRuntime().maxMemory());
        valores.put("carga", opciones.carga());
        valores.put("hilos", opciones.hilos());
        valores.put("modo", opciones.operacionesPorHilo() > 0 ? "operaciones_por_hilo" : "duracion");
        valores.put("segundosConfigurados", opciones.segundos());
        valores.put("calentamientoSegundos", opciones.calentamiento());
        valores.put("operacionesPorHiloConfiguradas", opciones.operacionesPorHilo());
        valores.put("duracionRealNs", resultado.duracionNs());
        valores.put("operaciones", resultado.operaciones());
        valores.put("idasVueltasCorrectas", resultado.correctas());
        valores.put("rechazosEsperados", resultado.rechazos());
        valores.put("erroresInesperados", resultado.errores());
        valores.put("primerTipoError", resultado.primerError());
        valores.put("operacionesPorSegundo", resultado.opsPorSegundo());
        valores.put("latenciaMinNs", resultado.latencias().minimo());
        valores.put("latenciaMediaNs", resultado.latencias().media());
        valores.put("latenciaMaxNs", resultado.latencias().maximo());
        valores.put("p50NsLimiteSuperior", resultado.latencias().percentil(.50));
        valores.put("p95NsLimiteSuperior", resultado.latencias().percentil(.95));
        valores.put("p99NsLimiteSuperior", resultado.latencias().percentil(.99));
        valores.put("heapAntesBytes", antes.heapUsadoBytes());
        valores.put("heapDespuesBytes", despues.heapUsadoBytes());
        valores.put("coleccionesGc", Math.max(0, despues.coleccionesGc() - antes.coleccionesGc()));
        valores.put("tiempoGcMs", Math.max(0, despues.tiempoGcMs() - antes.tiempoGcMs()));
        valores.put("umbralMinOps", opciones.minOps());
        valores.put("umbralMaxP99Ms", opciones.maxP99Ms());
        valores.put("cumple", resultado.cumple(opciones));
        var destino = opciones.salida().toAbsolutePath();
        Files.createDirectories(destino.getParent());
        String json = valores.entrySet().stream().map(entrada -> "  " + citar(entrada.getKey()) + ": " + literal(entrada.getValue()))
                .collect(Collectors.joining(",\n", "{\n", "\n}\n"));
        Files.writeString(destino, json, StandardCharsets.UTF_8);
        String cabeceras = valores.keySet().stream().map(ReporteEstres::csv).collect(Collectors.joining(","));
        String fila = valores.values().stream().map(valor -> csv(String.valueOf(valor))).collect(Collectors.joining(","));
        String nombreCsv = destino.getFileName().toString().replaceFirst("\\.json$", ".csv");
        Files.writeString(destino.resolveSibling(nombreCsv), cabeceras + "\n" + fila + "\n", StandardCharsets.UTF_8);
    }

    private static String literal(Object valor) { return valor instanceof String texto ? citar(texto) : String.valueOf(valor); }
    private static String csv(String texto) { return "\"" + texto.replace("\"", "\"\"") + "\""; }
    private static String citar(String texto) {
        var resultado = new StringBuilder("\"");
        for (char caracter : texto.toCharArray()) {
            switch (caracter) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (caracter < 32) { resultado.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) caracter)); }
                    else { resultado.append(caracter); }
                }
            }
        }
        return resultado.append('"').toString();
    }
}
