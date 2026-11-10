package org.gca.trama8583.escenarios;

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoEscenariosTest {
    @TestFactory
    Stream<DynamicTest> escenariosDeUso() {
        return CatalogoEscenarios.todos().stream().map(escenario ->
                DynamicTest.dynamicTest(escenario.toString(), escenario.verificar()::run));
    }

    @Test
    void identificadoresUnicosYContratosLegibles() {
        var catalogo = CatalogoEscenarios.todos();
        assertFalse(catalogo.isEmpty());
        assertEquals(catalogo.size(), catalogo.stream().map(Escenario::id).distinct().count());
        for (var escenario : catalogo) {
            assertTrue(escenario.id().matches("(NUC|ERR|ESP|SPR)-[0-9]{2}"));
            assertFalse(escenario.titulo().isBlank());
            assertFalse(escenario.dado().isBlank());
            assertFalse(escenario.cuando().isBlank());
            assertFalse(escenario.entonces().isBlank());
            assertNotNull(escenario.verificar());
        }
    }
}
