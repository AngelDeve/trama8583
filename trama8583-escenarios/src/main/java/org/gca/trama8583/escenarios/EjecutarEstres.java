package org.gca.trama8583.escenarios;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Carga concurrente instrumentada de laboratorio; no añade dependencias al núcleo. */
public final class EjecutarEstres {
    private EjecutarEstres() { }

    record Opciones(int hilos, int segundos, int calentamiento, long operacionesPorHilo,
                    String carga, Path salida, double minOps, double maxP99Ms) {
        static Opciones leer(String[] argumentos) {
            var valores = new HashMap<String, String>();
            var permitidos = List.of("--hilos", "--segundos", "--calentamiento", "--operaciones-por-hilo",
                    "--carga", "--salida", "--min-ops", "--max-p99-ms");
            for (int indice = 0; indice < argumentos.length; indice += 2) {
                String opcion = argumentos[indice];
                if (!permitidos.contains(opcion) || indice + 1 >= argumentos.length
                        || valores.putIfAbsent(opcion, argumentos[indice + 1]) != null) {
                    throw new IllegalArgumentException("Opciones desconocidas, repetidas o sin valor. Consulta --estres --ayuda.");
                }
            }
            int hilos = Integer.parseInt(valores.getOrDefault("--hilos", "4"));
            int segundos = Integer.parseInt(valores.getOrDefault("--segundos", "10"));
            int calentamiento = Integer.parseInt(valores.getOrDefault("--calentamiento", "2"));
            long operaciones = Long.parseLong(valores.getOrDefault("--operaciones-por-hilo", "0"));
            String carga = valores.getOrDefault("--carga", "mixta");
            Path salida = Path.of(valores.getOrDefault("--salida", "target/estres/resultado.json"));
            double minOps = Double.parseDouble(valores.getOrDefault("--min-ops", "0"));
            double maxP99 = Double.parseDouble(valores.getOrDefault("--max-p99-ms", "0"));
            if (hilos < 1 || hilos > 64 || segundos < 1 || segundos > 1800 || calentamiento < 0 || calentamiento > 60
                    || operaciones < 0 || operaciones > 100_000_000 || !CargasEstres.NOMBRES.contains(carga)
                    || !Double.isFinite(minOps) || minOps < 0 || !Double.isFinite(maxP99) || maxP99 < 0
                    || !salida.toString().endsWith(".json")) {
                throw new IllegalArgumentException("Usa hilos 1–64, segundos 1–1800, calentamiento 0–60, operaciones 0–100000000, "
                        + "carga válida, umbrales finitos no negativos y salida .json.");
            }
            return new Opciones(hilos, segundos, calentamiento, operaciones, carga, salida, minOps, maxP99);
        }
    }

    static final class ResultadoHilo {
        final HistogramaLatencia latencias = new HistogramaLatencia();
        long correctas;
        long rechazos;
        long errores;
        String primerError = "";
    }

    record Resultado(long duracionNs, long correctas, long rechazos, long errores,
                     HistogramaLatencia latencias, String primerError) {
        long operaciones() { return correctas + rechazos + errores; }
        double opsPorSegundo() { return operaciones() * 1_000_000_000.0 / duracionNs; }
        boolean cumple(Opciones opciones) {
            return operaciones() > 0 && errores == 0 && opsPorSegundo() >= opciones.minOps()
                    && (opciones.maxP99Ms() == 0 || latencias.percentil(.99) / 1_000_000.0 <= opciones.maxP99Ms());
        }
    }

    @FunctionalInterface
    interface Operacion { boolean ejecutar(long secuencia); }

    static Resultado medir(int hilos, long duracionNs, long operacionesPorHilo, Operacion operacion) throws Exception {
        var ejecutor = Executors.newFixedThreadPool(hilos, Thread.ofPlatform().daemon().name("estres-", 0).factory());
        var listos = new CountDownLatch(hilos);
        var inicio = new CountDownLatch(1);
        var futuros = new ArrayList<Future<ResultadoHilo>>();
        long[] instanteInicio = new long[1]; // Publicado a los trabajadores mediante CountDownLatch.
        try {
            for (int indice = 0; indice < hilos; indice++) {
                int trabajador = indice;
                futuros.add(ejecutor.submit(() -> {
                    var resultado = new ResultadoHilo();
                    listos.countDown();
                    inicio.await();
                    long realizadas = 0;
                    while (!Thread.currentThread().isInterrupted()
                            && (operacionesPorHilo > 0 ? realizadas < operacionesPorHilo
                            : System.nanoTime() - instanteInicio[0] < duracionNs)) {
                        long antes = System.nanoTime();
                        try {
                            // Paso impar: cada hilo alterna cargas y folios, incluso con 4/8/64 hilos.
                            if (operacion.ejecutar(realizadas * 67 + trabajador)) { resultado.rechazos++; }
                            else { resultado.correctas++; }
                        } catch (RuntimeException | AssertionError error) {
                            resultado.errores++;
                            if (resultado.primerError.isEmpty()) { resultado.primerError = error.getClass().getSimpleName(); }
                        }
                        resultado.latencias.registrar(System.nanoTime() - antes);
                        realizadas++;
                    }
                    return resultado;
                }));
            }
            if (!listos.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("No iniciaron todos los trabajadores"); }
            instanteInicio[0] = System.nanoTime();
            inicio.countDown();
            long limiteEspera = System.nanoTime() + (operacionesPorHilo > 0
                    ? TimeUnit.MINUTES.toNanos(10) : duracionNs + TimeUnit.SECONDS.toNanos(15));
            var latencias = new HistogramaLatencia();
            long correctas = 0, rechazos = 0, errores = 0;
            String primerError = "";
            for (var futuro : futuros) {
                var parcial = futuro.get(Math.max(1, limiteEspera - System.nanoTime()), TimeUnit.NANOSECONDS);
                correctas += parcial.correctas;
                rechazos += parcial.rechazos;
                errores += parcial.errores;
                latencias.combinar(parcial.latencias);
                if (primerError.isEmpty()) { primerError = parcial.primerError; }
            }
            return new Resultado(Math.max(1, System.nanoTime() - instanteInicio[0]), correctas, rechazos, errores, latencias, primerError);
        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
            if (!ejecutor.awaitTermination(5, TimeUnit.SECONDS)) { throw new IllegalStateException("La carga no terminó dentro del límite"); }
        }
    }

    public static int ejecutar(String[] argumentos, PrintStream salida, PrintStream errores) {
        if (argumentos.length == 1 && argumentos[0].equals("--listar")) {
            CargasEstres.NOMBRES.forEach(salida::println);
            return 0;
        }
        if (argumentos.length == 1 && argumentos[0].equals("--ayuda")) {
            salida.println("Uso: java -jar <jar> --estres [opción valor ...]");
            salida.println("--hilos 4 --segundos 10 --calentamiento 2 --carga mixta --salida target/estres/resultado.json");
            salida.println("--operaciones-por-hilo N sustituye el tiempo de medición por un conteo fijo (0 usa tiempo).");
            salida.println("Cargas: " + String.join(", ", CargasEstres.NOMBRES));
            salida.println("Umbrales opcionales: --min-ops N --max-p99-ms N. El valor 0 desactiva cada umbral.");
            salida.println("Salida: 0 cumple; 1 errores o umbral incumplido; 2 argumentos inválidos. Guarda JSON y CSV.");
            return 0;
        }
        final Opciones opciones;
        try {
            opciones = Opciones.leer(argumentos);
        } catch (IllegalArgumentException error) {
            errores.println("Argumentos inválidos: " + error.getMessage());
            return 2;
        }
        try (var cargas = new CargasEstres(opciones.carga())) {
            if (opciones.calentamiento() > 0) {
                var calentamiento = medir(opciones.hilos(), TimeUnit.SECONDS.toNanos(opciones.calentamiento()), 0, cargas::ejecutar);
                if (calentamiento.errores() > 0) { throw new IllegalStateException("Falló la validación durante el calentamiento"); }
            }
            var antes = ReporteEstres.estadoJvm();
            var resultado = medir(opciones.hilos(), TimeUnit.SECONDS.toNanos(opciones.segundos()), opciones.operacionesPorHilo(), cargas::ejecutar);
            var despues = ReporteEstres.estadoJvm();
            ReporteEstres.guardar(opciones, resultado, antes, despues);
            salida.printf(java.util.Locale.ROOT,
                    "Carga %s, %d hilos: %d operaciones, %.2f ops/s, p95 ≤ %.3f ms, p99 ≤ %.3f ms, %d errores, %d rechazos esperados.%n",
                    opciones.carga(), opciones.hilos(), resultado.operaciones(), resultado.opsPorSegundo(),
                    resultado.latencias().percentil(.95) / 1_000_000.0, resultado.latencias().percentil(.99) / 1_000_000.0,
                    resultado.errores(), resultado.rechazos());
            salida.println("Reportes JSON y CSV: " + opciones.salida().toAbsolutePath());
            if (!resultado.cumple(opciones)) { errores.println("No se cumplieron las comprobaciones o los umbrales configurados."); }
            return resultado.cumple(opciones) ? 0 : 1;
        } catch (Exception | AssertionError error) {
            if (error instanceof InterruptedException) { Thread.currentThread().interrupt(); }
            errores.println("No se pudo completar la carga: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            return 1;
        }
    }
}
