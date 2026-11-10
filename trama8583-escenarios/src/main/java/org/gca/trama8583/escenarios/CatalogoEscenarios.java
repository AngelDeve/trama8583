package org.gca.trama8583.escenarios;

import java.util.ArrayList;
import java.util.List;

/** Punto único de registro para consola y pruebas automáticas. */
public final class CatalogoEscenarios {
    private CatalogoEscenarios() { }

    public static List<Escenario> todos() {
        var escenarios = new ArrayList<Escenario>();
        escenarios.addAll(EscenariosNucleo.todos());
        escenarios.addAll(EscenariosErrores.todos());
        escenarios.addAll(EscenariosEspeciales.todos());
        escenarios.addAll(EscenariosSpring.todos());
        return List.copyOf(escenarios);
    }
}
