package org.gca.trama8583.escenarios;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Gramática inventada de laboratorio: etiqueta ASCII de dos bytes + longitud ASCII de dos bytes + valor.
 * No es un parser TLV del núcleo ni representa un formato de red. Los valores son opacos.
 */
final class ContenedorEjemplo {
    private ContenedorEjemplo() { }

    record Token(String etiqueta, byte[] valor) {
        Token { valor = valor.clone(); }
        @Override public byte[] valor() { return valor.clone(); }
    }

    static List<Token> leer(byte[] contenido) {
        if (contenido.length > 512) { throw invalido(); }
        var tokens = new ArrayList<Token>();
        var etiquetas = new HashSet<String>();
        int posicion = 0;
        while (posicion < contenido.length) {
            if (contenido.length - posicion < 4 || tokens.size() >= 16) { throw invalido(); }
            for (int indice = posicion; indice < posicion + 2; indice++) {
                if (contenido[indice] < 'A' || contenido[indice] > 'Z') { throw invalido(); }
            }
            String etiqueta = new String(contenido, posicion, 2, java.nio.charset.StandardCharsets.US_ASCII);
            if (!etiquetas.add(etiqueta)) { throw invalido(); }
            int longitud = 0;
            for (int indice = posicion + 2; indice < posicion + 4; indice++) {
                int digito = contenido[indice] - '0';
                if (digito < 0 || digito > 9) { throw invalido(); }
                longitud = longitud * 10 + digito;
            }
            posicion += 4;
            if (longitud > contenido.length - posicion) { throw invalido(); }
            tokens.add(new Token(etiqueta, java.util.Arrays.copyOfRange(contenido, posicion, posicion + longitud)));
            posicion += longitud;
        }
        return List.copyOf(tokens);
    }

    static byte[] escribir(List<Token> tokens) {
        if (tokens.size() > 16) { throw invalido(); }
        var salida = new ByteArrayOutputStream();
        for (var token : tokens) {
            if (token.valor().length > 99 || salida.size() + 4 + token.valor().length > 512) { throw invalido(); }
            salida.writeBytes(Comprobaciones.ascii(token.etiqueta()));
            int longitud = token.valor().length;
            salida.write('0' + longitud / 10);
            salida.write('0' + longitud % 10);
            salida.writeBytes(token.valor());
        }
        byte[] resultado = salida.toByteArray();
        leer(resultado); // Aplica los mismos límites y rechaza duplicados también al construir.
        return resultado;
    }

    private static IllegalArgumentException invalido() {
        return new IllegalArgumentException("CONTENEDOR_LAB_INVALIDO");
    }
}
