package org.gca.trama8583.escenarios;

import java.util.List;
import org.gca.trama8583.nucleo.*;
import static org.gca.trama8583.escenarios.Comprobaciones.*;
import static org.gca.trama8583.escenarios.DatosLaboratorio.*;

/** Casos sintéticos creados para enseñar variaciones; sin catálogos ni gramáticas de terceros. */
final class EscenariosEspeciales {
    private EscenariosEspeciales() { }

    static List<Escenario> todos() {
        return List.of(
                new Escenario("ESP-01", "Separar versión de cabecera y resultado del mensaje",
                        "Un prefijo DEMO y una cabecera inventada de cuatro bytes: versión + estado",
                        "Se codifica una respuesta con cabecera V1E1 y campo 39 igual a 00",
                        "El sobre se conserva independiente del resultado; su significado lo decide la aplicación", EscenariosEspeciales::cabeceraVersionada),
                new Escenario("ESP-02", "Cambiar montos fijos por montos variables con signo",
                        "Dos perfiles de laboratorio con campo 54 numérico fijo o texto LLVAR",
                        "Se envía 000000001250 o -0000012500 según el perfil",
                        "Cada vector conserva su representación y el perfil numérico rechaza el signo", EscenariosEspeciales::montos),
                new Escenario("ESP-03", "Transportar referencias de origen y destino",
                        "Campos 102 y 103 LLVAR con identificadores ficticios A0001 y B0002",
                        "Se construye e interpreta una solicitud con ambos campos",
                        "El secundario identifica ambos campos y sus longitudes se mantienen separadas", EscenariosEspeciales::cuentas),
                new Escenario("ESP-04", "Comprobar eco de solicitud y respuesta en la aplicación",
                        "Dos mensajes y una regla de ejemplo que compara campos 11 y 41",
                        "Se valida una respuesta correcta y otra con folio diferente",
                        "La primera pasa y la segunda identifica ECO_11 sin estado global", EscenariosEspeciales::eco),
                new Escenario("ESP-05", "Aplicar presencia condicionada por operación y resultado",
                        "Una regla inventada requiere monto en una consulta exitosa y lo prohíbe en un rechazo",
                        "La aplicación valida respuestas con y sin campo 54",
                        "Se identifican MONTO_REQUERIDO y MONTO_PROHIBIDO; el codec solo valida estructura", EscenariosEspeciales::condicionales),
                new Escenario("ESP-06", "Separar prefijo externo y longitudes internas",
                        "Un campo 48 LLLVAR contiene la gramática inventada AA03ABCBB02XY",
                        "Se interpreta la trama y luego el contenedor con el parser de ejemplo",
                        "El prefijo externo mide 13 bytes y cada longitud interna mide solo su valor", EscenariosEspeciales::compuesto),
                new Escenario("ESP-07", "Conservar tokens desconocidos como bytes opacos",
                        "Un contenedor inventado incluye etiqueta ZZ con bytes 00 FF",
                        "Se transporta por un campo binario 62 LLLVAR y se reconstruyen los tokens",
                        "Los bytes desconocidos se conservan sin interpretarlos como texto ni criptografía", EscenariosEspeciales::opacos),
                new Escenario("ESP-08", "Rechazar contenedores internos inconsistentes",
                        "Una gramática de ejemplo con límites de tamaño y etiquetas únicas",
                        "Se envían longitudes truncadas, etiquetas duplicadas, prefijos inválidos y exceso de tokens",
                        "El parser de aplicación rechaza cada contenido con un diagnóstico genérico", EscenariosEspeciales::compuestosInvalidos),
                new Escenario("ESP-09", "Representar reversos y mensajes de red",
                        "Un perfil de laboratorio para MTI 0400, 0410, 0800 y 0810",
                        "Se codifica e interpreta cada mensaje con folio y terminal sintéticos",
                        "Los vectores conservan el MTI; temporizadores, reintentos y correlación pertenecen a la aplicación", EscenariosEspeciales::familias),
                new Escenario("ESP-10", "Hacer explícito el relleno de un texto fijo",
                        "Un campo 43 de 40 bytes y un texto inventado REVERSO LAB",
                        "La aplicación agrega espacios antes de construir la trama",
                        "El codec conserva los 40 bytes y rechaza el texto corto sin rellenarlo", EscenariosEspeciales::relleno));
    }

    static void cabeceraVersionada() {
        var codec = new CodecIsoBasico(perfil().prefijoAscii("DEMO").longitudCabecera(4).construir());
        var respuesta = solicitud().copiar().mti("0210").campo(39, "00").cabecera(ascii("V1E1")).construir();
        byte[] esperado = concatenar(ascii("DEMOV1E1"), tramaRespuesta());
        bytes(esperado, codec.codificar(respuesta), "Debe conservar prefijo y cabecera de laboratorio");
        var recibido = codec.decodificar(esperado);
        mensaje(respuesta, recibido);
        igual("V1", new String(recibido.cabecera(), 0, 2, java.nio.charset.StandardCharsets.US_ASCII), "Debe conservar la versión");
        igual("00", recibido.texto(39), "El código del cuerpo debe conservarse independientemente del sobre");
    }

    static void montos() {
        var fijo = new CodecIsoBasico(PerfilMensaje.crear("monto-fijo-lab")
                .campo(DefinicionCampo.fijo(54, TipoDato.NUMERICO_ASCII, 12)).construir());
        var variable = new CodecIsoBasico(PerfilMensaje.crear("monto-variable-lab")
                .campo(DefinicionCampo.llvar(54, TipoDato.TEXTO_ASCII, 20)).construir());
        var mensajeFijo = MensajeIso.crear("0210").campo(54, "000000001250").construir();
        var mensajeVariable = MensajeIso.crear("0210").campo(54, "-0000012500").construir();
        byte[] esperadoFijo = ascii("0210" + "0000000000000400" + "000000001250");
        byte[] esperadoVariable = ascii("0210" + "0000000000000400" + "11-0000012500");
        bytes(esperadoFijo, fijo.codificar(mensajeFijo), "El monto fijo conserva sus dígitos");
        bytes(esperadoVariable, variable.codificar(mensajeVariable), "El monto de texto conserva el signo explícito");
        mensaje(mensajeFijo, fijo.decodificar(esperadoFijo));
        mensaje(mensajeVariable, variable.decodificar(esperadoVariable));
        var conSigno = MensajeIso.crear("0210").campo(54, "-00000001250").construir();
        error(CodigoErrorIso.CODIFICACION_INVALIDA, 54, -1, () -> fijo.codificar(conSigno));
    }

    static void cuentas() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("referencias-lab")
                .campo(DefinicionCampo.llvar(102, TipoDato.TEXTO_ASCII, 20))
                .campo(DefinicionCampo.llvar(103, TipoDato.TEXTO_ASCII, 20)).construir());
        var mensaje = MensajeIso.crear("0200").campo(102, "A0001").campo(103, "B0002").construir();
        byte[] esperado = ascii("0200" + "8000000000000000" + "0000000006000000" + "05A0001" + "05B0002");
        bytes(esperado, codec.codificar(mensaje), "Las referencias deben tener longitudes independientes");
        mensaje(mensaje, codec.decodificar(esperado));
    }

    static void eco() {
        var solicitud = solicitud();
        var respuesta = solicitud.copiar().mti("0210").campo(39, "00").construir();
        igual(List.of(), ReglasEjemplo.validar(solicitud, respuesta), "Debe aceptar el eco exacto");
        igual(List.of("ECO_11"), ReglasEjemplo.validar(solicitud, respuesta.copiar().campo(11, "000002").construir()),
                "Debe detectar el cambio de folio");
        igual(List.of("ECO_41"), ReglasEjemplo.validar(solicitud, respuesta.copiar().quitarCampo(41).construir()),
                "Debe detectar un eco ausente");
    }

    static void condicionales() {
        var codec = new CodecIsoBasico(perfil().campo(DefinicionCampo.fijo(54, TipoDato.NUMERICO_ASCII, 12)).construir());
        var consulta = solicitud().copiar().campo(3, "310000").construir();
        var respuesta = consulta.copiar().mti("0210").campo(39, "00").construir();
        // La estructura es válida sin monto; la condición es responsabilidad de la aplicación.
        mensaje(respuesta, codec.decodificar(codec.codificar(respuesta)));
        igual(List.of("MONTO_REQUERIDO"), ReglasEjemplo.validar(consulta, respuesta), "La consulta exitosa requiere monto");
        var completa = respuesta.copiar().campo(54, "000000001250").construir();
        igual(List.of(), ReglasEjemplo.validar(consulta, completa), "Debe aceptar el monto cuando aplica");
        igual(List.of("MONTO_PROHIBIDO"), ReglasEjemplo.validar(consulta, completa.copiar().campo(39, "05").construir()),
                "El rechazo no debe incluir el monto según esta regla inventada");
        igual(List.of(), ReglasEjemplo.validar(consulta, respuesta.copiar().campo(39, "05").construir()), "El rechazo sin monto es válido");
    }

    static void compuesto() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("compuesto-lab")
                .campo(DefinicionCampo.lllvar(48, TipoDato.TEXTO_ASCII, 512)).construir());
        var mensaje = MensajeIso.crear("0200").campo(48, "AA03ABCBB02XY").construir();
        byte[] esperado = ascii("0200" + "0000000000010000" + "013AA03ABCBB02XY");
        bytes(esperado, codec.codificar(mensaje), "El prefijo externo debe contar todo el contenedor");
        var tokens = ContenedorEjemplo.leer(ascii(codec.decodificar(esperado).texto(48)));
        igual(2, tokens.size(), "Debe obtener dos tokens de laboratorio");
        igual("AA", tokens.getFirst().etiqueta(), "Debe conservar la primera etiqueta");
        bytes(ascii("ABC"), tokens.getFirst().valor(), "La longitud interna excluye la etiqueta y su prefijo");
        bytes(ascii("XY"), tokens.get(1).valor(), "Debe conservar el segundo contenido");
        bytes(ascii(mensaje.texto(48)), ContenedorEjemplo.escribir(tokens), "Debe reconstruir exactamente el contenedor");
    }

    static void opacos() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("opacos-lab")
                .campo(DefinicionCampo.lllvar(62, TipoDato.BINARIO, 512)).construir());
        byte[] contenido = concatenar(ascii("ZZ02"), new byte[]{0, (byte) 0xFF});
        var mensaje = MensajeIso.crear("0200").campoBinario(62, contenido).construir();
        byte[] esperado = concatenar(ascii("0200" + "0000000000000004" + "006"), contenido);
        bytes(esperado, codec.codificar(mensaje), "Debe transportar los bytes sin transformación");
        byte[] recibido = ((ValorBinario) codec.decodificar(esperado).campo(62)).bytes();
        bytes(contenido, ContenedorEjemplo.escribir(ContenedorEjemplo.leer(recibido)), "Debe conservar tokens de etiqueta desconocida");
    }

    static void compuestosInvalidos() {
        String demasiadosTokens = java.util.stream.IntStream.range(0, 17)
                .mapToObj(indice -> "" + (char) ('A' + indice) + "A00").collect(java.util.stream.Collectors.joining());
        for (String contenido : List.of("AA99ABC", "AA01XAA01Y", "AA0XQ", "A", demasiadosTokens, " ".repeat(513))) {
            try {
                ContenedorEjemplo.leer(ascii(contenido));
            } catch (IllegalArgumentException error) {
                igual("CONTENEDOR_LAB_INVALIDO", error.getMessage(), "Debe informar solo el diagnóstico del contenedor");
                continue;
            }
            throw new AssertionError("Debe rechazar el contenedor inconsistente");
        }
    }

    static void familias() {
        var constructor = PerfilMensaje.crear("familias-lab")
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8));
        for (String mti : List.of("0400", "0410", "0800", "0810")) { constructor.obligatorios(mti, 11, 41); }
        var codec = new CodecIsoBasico(constructor.construir());
        for (String mti : List.of("0400", "0410", "0800", "0810")) {
            var mensaje = MensajeIso.crear(mti).campo(11, "000001").campo(41, "TERM0001").construir();
            byte[] esperado = ascii(mti + "0020000000800000" + "000001TERM0001");
            bytes(esperado, codec.codificar(mensaje), "Debe conservar el MTI configurado");
            mensaje(mensaje, codec.decodificar(esperado));
        }
    }

    static void relleno() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("texto-fijo-lab")
                .campo(DefinicionCampo.fijo(43, TipoDato.TEXTO_ASCII, 40)).construir());
        String texto = "REVERSO LAB";
        String rellenado = texto + " ".repeat(40 - texto.length());
        var mensaje = MensajeIso.crear("0400").campo(43, rellenado).construir();
        byte[] esperado = ascii("0400" + "0000000000200000" + rellenado);
        bytes(esperado, codec.codificar(mensaje), "Debe conservar los espacios agregados por la aplicación");
        mensaje(mensaje, codec.decodificar(esperado));
        error(CodigoErrorIso.LONGITUD_INVALIDA, 43, -1, () -> codec.codificar(MensajeIso.crear("0400").campo(43, texto).construir()));
    }
}
