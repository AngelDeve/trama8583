package org.gca.trama8583.escenarios;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Ejecutor local: salida 0 si todo pasa, 1 si falla un caso, 2 si los argumentos son inválidos. */
public final class EjecutarEscenarios {
    private EjecutarEscenarios() { }

    public static void main(String[] argumentos) {
        // Mantiene visibles las advertencias sin ocultar el relato de cada escenario entre logs de arranque.
        java.util.logging.Logger.getLogger("org.springframework").setLevel(java.util.logging.Level.WARNING);
        if (argumentos.length > 0 && argumentos[0].equals("--estres")) {
            System.exit(EjecutarEstres.ejecutar(java.util.Arrays.copyOfRange(argumentos, 1, argumentos.length), System.out, System.err));
        } else {
            System.exit(ejecutar(argumentos, CatalogoEscenarios.todos(), System.out, System.err));
        }
    }

    static int ejecutar(String[] argumentos, List<Escenario> catalogo, PrintStream salida, PrintStream errores) {
        if (argumentos.length == 1 && argumentos[0].equals("--ayuda")) {
            salida.println("Uso: java -jar trama8583-escenarios-1.0-SNAPSHOT.jar [--listar | --todos | ID ...]");
            salida.println("Sin argumentos ejecuta todos. Ejemplo de selección: NUC-01 ERR-04 SPR-01");
            salida.println("Pruebas de carga: --estres --ayuda");
            salida.println("Códigos de salida: 0 = correcto; 1 = escenario fallido; 2 = argumentos inválidos.");
            return 0;
        }
        if (argumentos.length == 1 && argumentos[0].equals("--listar")) {
            catalogo.forEach(salida::println);
            return 0;
        }
        var seleccion = new ArrayList<Escenario>();
        if (argumentos.length == 0 || (argumentos.length == 1 && argumentos[0].equals("--todos"))) {
            seleccion.addAll(catalogo);
        } else {
            // Valida toda la selección antes de ejecutar para evitar una ejecución parcial por un ID erróneo.
            for (String argumento : argumentos) {
                var escenario = catalogo.stream().filter(caso -> caso.id().equals(argumento)).findFirst();
                if (escenario.isEmpty()) {
                    errores.println("Escenario o argumento desconocido: " + argumento + ". Consulta --listar o --ayuda.");
                    return 2;
                }
                if (!seleccion.contains(escenario.get())) { seleccion.add(escenario.get()); }
            }
        }

        int fallidos = 0;
        for (var escenario : seleccion) {
            salida.println();
            salida.println(escenario);
            salida.println("  Dado: " + escenario.dado());
            salida.println("  Cuando: " + escenario.cuando());
            salida.println("  Entonces: " + escenario.entonces());
            try {
                escenario.verificar().run();
                salida.println("  CORRECTO");
            } catch (RuntimeException | AssertionError error) {
                fallidos++;
                errores.println("FALLÓ " + escenario.id() + ": " + error.getMessage());
                error.printStackTrace(errores);
            }
        }
        salida.printf("%nResultado: %d correctos, %d fallidos, %d ejecutados.%n",
                seleccion.size() - fallidos, fallidos, seleccion.size());
        return fallidos == 0 ? 0 : 1;
    }
}
