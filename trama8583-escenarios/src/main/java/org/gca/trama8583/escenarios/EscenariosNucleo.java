package org.gca.trama8583.escenarios;

import java.util.HexFormat;
import java.util.List;
import org.gca.trama8583.nucleo.CodecIsoBasico;
import org.gca.trama8583.nucleo.DefinicionCampo;
import org.gca.trama8583.nucleo.FormatoBitmap;
import org.gca.trama8583.nucleo.MensajeIso;
import org.gca.trama8583.nucleo.PerfilMensaje;
import org.gca.trama8583.nucleo.TipoDato;
import static org.gca.trama8583.escenarios.Comprobaciones.*;
import static org.gca.trama8583.escenarios.DatosLaboratorio.*;

/** Recetas del núcleo: cada método prepara, utiliza y comprueba una capacidad. */
final class EscenariosNucleo {
    private EscenariosNucleo() { }

    static List<Escenario> todos() {
        return List.of(
                new Escenario("NUC-01", "Construir e interpretar una solicitud",
                        "Un perfil ASCII con campos 3, 11 y 41 obligatorios para 0200",
                        "Se codifica la solicitud con campos insertados fuera de orden y se lee un vector conocido",
                        "La trama tiene 40 bytes exactos y conserva MTI, ceros y campos", EscenariosNucleo::solicitudConocida),
                new Escenario("NUC-02", "Derivar una respuesta sin modificar la solicitud",
                        "Una solicitud inmutable de laboratorio",
                        "Se copia a MTI 0210 y se agrega el campo 39 con valor sintético 00",
                        "La respuesta coincide con el vector de 42 bytes y la solicitud queda intacta", EscenariosNucleo::respuesta),
                new Escenario("NUC-03", "Usar campos LLVAR y LLLVAR",
                        "Campos de texto 48 LLVAR y 60 LLLVAR con máximos explícitos",
                        "Se envían PEDIDO-42 y CAJA=07;TURNO=2",
                        "Los prefijos son 09 y 015 y se conserva el contenido", EscenariosNucleo::longitudesVariables),
                new Escenario("NUC-04", "Transportar bytes opacos y activar el bitmap secundario",
                        "Un campo 128 binario de cuatro bytes, sin significado criptográfico",
                        "Se codifican los bytes 00 7F 80 FF",
                        "Se activa el secundario y los bytes se conservan sin conversión de texto", EscenariosNucleo::binarioSecundario),
                new Escenario("NUC-05", "Cambiar a bitmap binario",
                        "El mismo perfil de solicitud con FormatoBitmap.BINARIO",
                        "Se construye e interpreta una solicitud",
                        "El bitmap ocupa ocho bytes y la trama completa tiene 32 bytes", EscenariosNucleo::bitmapBinario),
                new Escenario("NUC-06", "Agregar prefijo y cabecera opaca",
                        "Un prefijo constante LAB y una cabecera de tres bytes",
                        "Se codifica y decodifica la solicitud con cabecera 01 02 03",
                        "El sobre precede al MTI y la cabecera se conserva", EscenariosNucleo::sobre),
                new Escenario("NUC-07", "Adaptar la longitud de terminal por perfil",
                        "Un constructor con campo 41 de ocho bytes y un perfil ya construido",
                        "Se reemplaza explícitamente la definición por 16 bytes y se construye otra variante",
                        "Cada perfil conserva su longitud y produce su vector correspondiente", EscenariosNucleo::terminalExtendida),
                new Escenario("NUC-08", "Exigir un bitmap secundario vacío",
                        "Un perfil con secundarioSiempre aunque solo hay campos del primario",
                        "Se codifica una solicitud sin campos superiores a 64",
                        "El secundario vacío se emite y se interpreta según el perfil", EscenariosNucleo::secundarioObligatorio),
                new Escenario("NUC-09", "Reutilizar datos sin compartir arreglos mutables",
                        "Un mensaje construido a partir de bytes binarios y una cabecera",
                        "Se modifican los arreglos de entrada, los de salida y una copia del mensaje",
                        "La instantánea original conserva campos, cabecera y bytes codificados", EscenariosNucleo::inmutabilidad));
    }

    static void solicitudConocida() {
        var codec = new CodecIsoBasico(perfil().construir());
        var solicitud = solicitud();
        bytes(tramaSolicitud(), codec.codificar(solicitud), "Debe producir el vector de solicitud conocido");
        mensaje(solicitud, codec.decodificar(tramaSolicitud()));
    }

    static void respuesta() {
        var codec = new CodecIsoBasico(perfil().construir());
        var solicitud = solicitud();
        // 00 es una respuesta sintética; esta biblioteca no autoriza operaciones.
        var respuesta = solicitud.copiar().mti("0210").campo(39, "00").construir();
        bytes(tramaRespuesta(), codec.codificar(respuesta), "Debe producir el vector de respuesta conocido");
        mensaje(respuesta, codec.decodificar(tramaRespuesta()));
        igual("0200", solicitud.mti(), "La solicitud debe conservar su MTI");
        igual(null, solicitud.campo(39), "La solicitud no debe adquirir el campo 39 de la copia");
        bytes(tramaSolicitud(), codec.codificar(solicitud), "La solicitud debe permanecer intacta");
    }

    static void longitudesVariables() {
        var perfil = PerfilMensaje.crear("variables-v1")
                .campo(DefinicionCampo.llvar(48, TipoDato.TEXTO_ASCII, 30))
                .campo(DefinicionCampo.lllvar(60, TipoDato.TEXTO_ASCII, 120)).construir();
        var codec = new CodecIsoBasico(perfil);
        var mensaje = MensajeIso.crear("0200").campo(48, "PEDIDO-42").campo(60, "CAJA=07;TURNO=2").construir();
        byte[] esperado = ascii("0200" + "0000000000010010" + "09PEDIDO-42" + "015CAJA=07;TURNO=2");
        bytes(esperado, codec.codificar(mensaje), "Debe anteponer las longitudes decimales ASCII");
        mensaje(mensaje, codec.decodificar(esperado));
    }

    static void binarioSecundario() {
        var perfil = PerfilMensaje.crear("binario-secundario-v1")
                .campo(DefinicionCampo.fijo(128, TipoDato.BINARIO, 4)).construir();
        var codec = new CodecIsoBasico(perfil);
        byte[] contenido = {0, 0x7F, (byte) 0x80, (byte) 0xFF};
        var mensaje = MensajeIso.crear("0800").campoBinario(128, contenido).construir();
        byte[] esperado = concatenar(ascii("0800" + "8000000000000000" + "0000000000000001"), contenido);
        bytes(esperado, codec.codificar(mensaje), "Debe activar los bits 1 y 128");
        mensaje(mensaje, codec.decodificar(esperado));
    }

    static void bitmapBinario() {
        var codec = new CodecIsoBasico(perfil().formatoBitmap(FormatoBitmap.BINARIO).construir());
        byte[] esperado = concatenar(ascii("0200"), HexFormat.of().parseHex("2020000000800000"),
                ascii("000000" + "000001" + "TERM0001"));
        igual(32, esperado.length, "La solicitud con bitmap binario debe ocupar 32 bytes");
        bytes(esperado, codec.codificar(solicitud()), "Debe escribir ocho bytes de bitmap binario");
        mensaje(solicitud(), codec.decodificar(esperado));
    }

    static void sobre() {
        var codec = new CodecIsoBasico(perfil().prefijoAscii("LAB").longitudCabecera(3).construir());
        byte[] cabecera = {1, 2, 3};
        var mensaje = solicitud().copiar().cabecera(cabecera).construir();
        byte[] esperado = concatenar(ascii("LAB"), cabecera, tramaSolicitud());
        bytes(esperado, codec.codificar(mensaje), "El orden debe ser prefijo, cabecera, MTI, bitmap y campos");
        mensaje(mensaje, codec.decodificar(esperado));
    }

    static void terminalExtendida() {
        var constructor = perfil();
        var original = new CodecIsoBasico(constructor.construir());
        var extendido = new CodecIsoBasico(constructor
                .reemplazarCampo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 16)).construir());
        var mensaje = solicitud().copiar().campo(41, "TERM000000000001").construir();
        byte[] esperado = ascii("0200" + "2020000000800000" + "000000" + "000001" + "TERM000000000001");
        bytes(esperado, extendido.codificar(mensaje), "Debe utilizar 16 bytes para la terminal");
        mensaje(mensaje, extendido.decodificar(esperado));
        bytes(tramaSolicitud(), original.codificar(solicitud()), "El perfil original debe seguir aceptando ocho bytes");
    }

    static void secundarioObligatorio() {
        var codec = new CodecIsoBasico(perfil().secundarioSiempre(true).construir());
        byte[] esperado = ascii("0200" + "A020000000800000" + "0000000000000000"
                + "000000" + "000001" + "TERM0001");
        bytes(esperado, codec.codificar(solicitud()), "Debe emitir un secundario vacío y activar el bit 1");
        mensaje(solicitud(), codec.decodificar(esperado));
    }

    static void inmutabilidad() {
        var perfil = PerfilMensaje.crear("instantaneas-v1").longitudCabecera(1)
                .campo(DefinicionCampo.fijo(64, TipoDato.BINARIO, 2)).construir();
        var codec = new CodecIsoBasico(perfil);
        byte[] entrada = {1, 2};
        byte[] cabecera = {3};
        var original = MensajeIso.crear("0800").cabecera(cabecera).campoBinario(64, entrada).construir();
        byte[] esperado = concatenar(new byte[]{3}, ascii("0800" + "0000000000000001"), new byte[]{1, 2});
        entrada[0] = 99;
        cabecera[0] = 99;
        original.cabecera()[0] = 99;
        ((org.gca.trama8583.nucleo.ValorBinario) original.campo(64)).bytes()[0] = 99;
        var copia = original.copiar().mti("0810").quitarCampo(64).construir();
        igual("0810", copia.mti(), "La copia debe permitir un MTI propio");
        igual(0, copia.campos().size(), "La copia debe permitir quitar campos");
        bytes(esperado, codec.codificar(original), "Los arreglos y la copia no deben modificar el original");
        mensaje(original, codec.decodificar(esperado));
    }
}
