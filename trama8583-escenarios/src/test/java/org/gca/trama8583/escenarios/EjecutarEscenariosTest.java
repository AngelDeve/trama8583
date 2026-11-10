package org.gca.trama8583.escenarios;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EjecutarEscenariosTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errores = new ByteArrayOutputStream();

    private int ejecutar(List<Escenario> catalogo, String... argumentos) {
        try (var texto = new PrintStream(salida, true, StandardCharsets.UTF_8);
             var diagnosticos = new PrintStream(errores, true, StandardCharsets.UTF_8)) {
            return EjecutarEscenarios.ejecutar(argumentos, catalogo, texto, diagnosticos);
        }
    }

    private Escenario caso(String id, Runnable accion) {
        return new Escenario(id, "Caso de prueba", "Preparación", "Acción", "Resultado", accion);
    }

    @Test void listarNoEjecutaEscenarios() {
        assertEquals(0, ejecutar(List.of(caso("NUC-01", () -> fail("No debe ejecutar al listar"))), "--listar"));
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains("NUC-01"));
    }

    @Test void argumentoInvalidoEvitaEjecucionParcial() {
        assertEquals(2, ejecutar(List.of(caso("NUC-01", () -> fail("No debe ejecutar con selección inválida"))),
                "NUC-01", "NO-EXISTE"));
        assertTrue(errores.toString(StandardCharsets.UTF_8).contains("NO-EXISTE"));
    }

    @Test void seleccionRespetaOrdenYNoRepiteIds() {
        var ejecutados = new ArrayList<String>();
        var catalogo = List.of(caso("NUC-01", () -> ejecutados.add("primero")),
                caso("ERR-01", () -> ejecutados.add("segundo")), caso("SPR-01", () -> fail("No fue seleccionado")));
        assertEquals(0, ejecutar(catalogo, "ERR-01", "NUC-01", "ERR-01"));
        assertEquals(List.of("segundo", "primero"), ejecutados);
    }

    @Test void falloProduceSalidaNoCeroYPermiteCompletarLosDemas() {
        var ejecutados = new ArrayList<String>();
        var catalogo = List.of(caso("NUC-01", () -> { throw new AssertionError("Contrato alterado"); }),
                caso("ERR-01", () -> { throw new IllegalStateException("Fallo inesperado"); }),
                caso("SPR-01", () -> ejecutados.add("continuó")));
        assertEquals(1, ejecutar(catalogo));
        assertEquals(List.of("continuó"), ejecutados);
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains("1 correctos, 2 fallidos, 3 ejecutados"));
        assertTrue(errores.toString(StandardCharsets.UTF_8).contains("NUC-01"));
        assertTrue(errores.toString(StandardCharsets.UTF_8).contains("ERR-01"));
    }
}
