package org.gca.trama8583.nucleo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Escenarios del contrato de bytes; las tramas esperadas no proceden del codec. */
class CodecEscenariosTest {
    private static byte[] ascii(String texto) { return texto.getBytes(StandardCharsets.US_ASCII); }
    private static byte[] unir(byte[]... bloques) {
        var salida = new ByteArrayOutputStream();
        for (byte[] bloque : bloques) { salida.writeBytes(bloque); }
        return salida.toByteArray();
    }
    private static byte[] bitmap(FormatoBitmap formato, String hexadecimal) {
        return formato == FormatoBitmap.BINARIO ? HexFormat.of().parseHex(hexadecimal) : ascii(hexadecimal);
    }
    private static ExcepcionIso verificarError(CodigoErrorIso codigo, Integer campo, int posicion, Runnable accion) {
        var error = assertThrows(ExcepcionIso.class, accion::run);
        assertAll(() -> assertEquals(codigo, error.codigo()), () -> assertEquals(campo, error.campo()),
                () -> assertEquals(posicion, error.posicion()));
        return error;
    }

    static Stream<Arguments> combinacionesValidas() {
        var casos = new ArrayList<Arguments>();
        // Longitudes y prefijos independientes para detectar errores de ceros y limites.
        Object[][] longitudes = {{TipoLongitud.FIJA, 3, ""}, {TipoLongitud.LLVAR, 0, "00"},
                {TipoLongitud.LLVAR, 1, "01"}, {TipoLongitud.LLVAR, 99, "99"},
                {TipoLongitud.LLLVAR, 0, "000"}, {TipoLongitud.LLLVAR, 1, "001"},
                {TipoLongitud.LLLVAR, 100, "100"}, {TipoLongitud.LLLVAR, 999, "999"}};
        for (var formato : FormatoBitmap.values()) {
            for (boolean secundario : new boolean[]{false, true}) {
                for (var tipo : TipoDato.values()) {
                    for (var longitud : longitudes) {
                        casos.add(Arguments.of(formato, secundario, tipo, longitud[0], longitud[1], longitud[2]));
                    }
                }
            }
        }
        return casos.stream();
    }
    @ParameterizedTest(name = "{0}, secundario={1}, {2}, {3}, contenido={4} bytes")
    @MethodSource("combinacionesValidas")
    void formatosTiposYLongitudesProducenBytesExactos(FormatoBitmap formato, boolean secundario,
            TipoDato tipo, TipoLongitud tipoLongitud, int longitud, String prefijoLongitud) {
        int maximo = switch (tipoLongitud) { case FIJA -> 3; case LLVAR -> 99; case LLLVAR -> 999; };
        var perfil = PerfilMensaje.crear("matriz").formatoBitmap(formato).secundarioSiempre(secundario)
                .campo(new DefinicionCampo(2, tipo, tipoLongitud, maximo)).construir();
        byte[] contenido = new byte[longitud];
        Arrays.fill(contenido, (byte) (tipo == TipoDato.NUMERICO_ASCII ? '0' : tipo == TipoDato.TEXTO_ASCII ? ' ' : 255));
        var constructor = MensajeIso.crear("0200");
        if (tipo == TipoDato.BINARIO) { constructor.campoBinario(2, contenido); }
        else { constructor.campo(2, new String(contenido, StandardCharsets.US_ASCII)); }
        var mensaje = constructor.construir();
        byte[] esperado = unir(ascii("0200"), bitmap(formato, secundario
                ? "C0000000000000000000000000000000" : "4000000000000000"), ascii(prefijoLongitud), contenido);
        var codec = new CodecIsoBasico(perfil);
        assertArrayEquals(esperado, codec.codificar(mensaje));
        var recibido = codec.decodificar(esperado);
        assertEquals(mensaje.campos(), recibido.campos());
        assertEquals("0200", recibido.mti());
        assertArrayEquals(esperado, codec.codificar(recibido));
    }

    static Stream<Arguments> longitudesInvalidas() {
        return Stream.of(Arguments.of(TipoLongitud.FIJA, 0), Arguments.of(TipoLongitud.FIJA, 2),
                Arguments.of(TipoLongitud.FIJA, 4), Arguments.of(TipoLongitud.LLVAR, 4),
                Arguments.of(TipoLongitud.LLVAR, 100), Arguments.of(TipoLongitud.LLLVAR, 4),
                Arguments.of(TipoLongitud.LLLVAR, 1000));
    }
    @ParameterizedTest(name = "{0}: rechaza {1} bytes con longitud declarada 3")
    @MethodSource("longitudesInvalidas")
    void rechazaContenidoFueraDeLongitudAlCodificar(TipoLongitud formato, int longitud) {
        for (var tipo : TipoDato.values()) {
            var codec = new CodecIsoBasico(PerfilMensaje.crear("longitud")
                    .campo(new DefinicionCampo(2, tipo, formato, 3)).construir());
            var constructor = MensajeIso.crear("0200");
            if (tipo == TipoDato.BINARIO) { constructor.campoBinario(2, new byte[longitud]); }
            else { constructor.campo(2, "0".repeat(longitud)); }
            verificarError(CodigoErrorIso.LONGITUD_INVALIDA, 2, -1, () -> codec.codificar(constructor.construir()));
        }
    }

    private static PerfilMensaje perfilSobre(FormatoBitmap formato) {
        return PerfilMensaje.crear("sobre").formatoBitmap(formato).prefijoAscii("ISO").longitudCabecera(2)
                .campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 5))
                .campo(DefinicionCampo.lllvar(48, TipoDato.BINARIO, 5))
                .campo(DefinicionCampo.fijo(70, TipoDato.NUMERICO_ASCII, 3)).construir();
    }
    private static byte[] tramaSobre(FormatoBitmap formato) {
        return unir(ascii("ISO"), new byte[]{1, 2}, ascii("0800"),
                bitmap(formato, "C0000000000100000400000000000000"), ascii("03ABC002"),
                new byte[]{0, (byte) 255}, ascii("301"));
    }
    static Stream<Arguments> truncamientos() {
        var casos = new ArrayList<Arguments>();
        for (var formato : FormatoBitmap.values()) {
            int bloque = formato == FormatoBitmap.BINARIO ? 8 : 16;
            int[] tamanos = {3, 2, 4, bloque, bloque, 2, 3, 3, 2, 3};
            String[] nombres = {"prefijo", "cabecera", "MTI", "bitmap primario", "bitmap secundario",
                    "prefijo LLVAR", "contenido LLVAR", "prefijo LLLVAR", "contenido LLLVAR", "campo fijo"};
            Integer[] campos = {null, null, null, null, null, 2, 2, 48, 48, 70};
            int inicio = 0;
            for (int segmento = 0; segmento < tamanos.length; segmento++) {
                for (int desplazamiento = 0; desplazamiento < tamanos[segmento]; desplazamiento++) {
                    casos.add(Arguments.of(formato, nombres[segmento], inicio + desplazamiento, campos[segmento], inicio));
                }
                inicio += tamanos[segmento];
            }
        }
        return casos.stream();
    }
    @ParameterizedTest(name = "{0}: truncamiento en {1}, corte={2}, campo={3}, posicion={4}")
    @MethodSource("truncamientos")
    void cadaTruncamientoIdentificaSegmentoYCampo(FormatoBitmap formato, String segmento,
            int corte, Integer campo, int posicion) {
        byte[] truncada = Arrays.copyOf(tramaSobre(formato), corte);
        verificarError(CodigoErrorIso.TRAMA_INCOMPLETA, campo, posicion,
                () -> new CodecIsoBasico(perfilSobre(formato)).decodificar(truncada));
    }
    @ParameterizedTest @EnumSource(FormatoBitmap.class)
    void sobreValidoYArreglosNoCompartenContenido(FormatoBitmap formato) {
        var codec = new CodecIsoBasico(perfilSobre(formato));
        byte[] trama = tramaSobre(formato);
        byte[] original = trama.clone();
        var mensaje = codec.decodificar(trama);
        assertArrayEquals(original, trama);
        assertArrayEquals(original, codec.codificar(mensaje));
        Arrays.fill(trama, (byte) 0);
        assertEquals("ABC", mensaje.texto(2));
        assertArrayEquals(new byte[]{1, 2}, mensaje.cabecera());
        assertArrayEquals(new byte[]{0, (byte) 255}, ((ValorBinario) mensaje.campo(48)).bytes());
        byte[] salida = codec.codificar(mensaje); Arrays.fill(salida, (byte) 0);
        assertArrayEquals(original, codec.codificar(mensaje));
    }

    static Stream<Arguments> erroresConSobre() {
        return Stream.of(
                Arguments.of("literal incorrecto", 0, 'X', CodigoErrorIso.SOBRE_INVALIDO, null, 0),
                Arguments.of("MTI incorrecto", 6, 'A', CodigoErrorIso.MTI_INVALIDO, null, 5),
                Arguments.of("hex minuscula primario", 9, 'c', CodigoErrorIso.BITMAP_INVALIDO, null, 9),
                Arguments.of("hex minuscula secundario", 27, 'a', CodigoErrorIso.BITMAP_INVALIDO, null, 27),
                Arguments.of("LLVAR no decimal", 42, 'A', CodigoErrorIso.LONGITUD_INVALIDA, 2, 41),
                Arguments.of("LLVAR excedido", 42, '6', CodigoErrorIso.LONGITUD_INVALIDA, 2, 41),
                Arguments.of("texto no ASCII", 44, 255, CodigoErrorIso.CODIFICACION_INVALIDA, 2, 44),
                Arguments.of("LLLVAR no decimal", 47, '-', CodigoErrorIso.LONGITUD_INVALIDA, 48, 46),
                Arguments.of("LLLVAR excedido", 48, '6', CodigoErrorIso.LONGITUD_INVALIDA, 48, 46),
                Arguments.of("numero no decimal", 52, 'A', CodigoErrorIso.CODIFICACION_INVALIDA, 70, 52));
    }
    @ParameterizedTest(name = "{0}") @MethodSource("erroresConSobre")
    void erroresIncluyenPosicionAbsoluta(String escenario, int indice, int byteInvalido,
            CodigoErrorIso codigo, Integer campo, int posicion) {
        byte[] trama = tramaSobre(FormatoBitmap.HEXADECIMAL_ASCII); trama[indice] = (byte) byteInvalido;
        verificarError(codigo, campo, posicion,
                () -> new CodecIsoBasico(perfilSobre(FormatoBitmap.HEXADECIMAL_ASCII)).decodificar(trama));
    }
    @ParameterizedTest @EnumSource(FormatoBitmap.class)
    void rechazaCampoSecundarioDesconocidoYBytesSobrantes(FormatoBitmap formato) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("vacio").formatoBitmap(formato).construir());
        byte[] desconocido = unir(ascii("0800"), bitmap(formato, "80000000000000000000000000000001"));
        verificarError(CodigoErrorIso.CAMPO_NO_DEFINIDO, 128, desconocido.length, () -> codec.decodificar(desconocido));
        verificarError(CodigoErrorIso.CAMPO_NO_DEFINIDO, 128, -1,
                () -> codec.codificar(MensajeIso.crear("0800").campo(128, "X").construir()));
        var codecSobre = new CodecIsoBasico(perfilSobre(formato));
        byte[] trama = tramaSobre(formato);
        verificarError(CodigoErrorIso.DATOS_SOBRANTES, null, trama.length,
                () -> codecSobre.decodificar(unir(trama, new byte[]{0})));
    }

    @ParameterizedTest @EnumSource(FormatoBitmap.class)
    void aceptaLimiteExactoYRechazaUnByteMas(FormatoBitmap formato) {
        byte[] esperado = unir(ascii("0200"), bitmap(formato, "4000000000000000"), ascii("03ABC"));
        var codec = new CodecIsoBasico(PerfilMensaje.crear("limite").formatoBitmap(formato)
                .maximoBytes(esperado.length).campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 4)).construir());
        var mensaje = MensajeIso.crear("0200").campo(2, "ABC").construir();
        assertArrayEquals(esperado, codec.codificar(mensaje));
        assertEquals("ABC", codec.decodificar(esperado).texto(2));
        verificarError(CodigoErrorIso.LIMITE_EXCEDIDO, 2, -1,
                () -> codec.codificar(mensaje.copiar().campo(2, "ABCD").construir()));
        verificarError(CodigoErrorIso.LIMITE_EXCEDIDO, null, 0,
                () -> codec.decodificar(unir(esperado, new byte[]{0})));
    }

    @ParameterizedTest @ValueSource(strings = {"0000", "9999"})
    void mtiEsEstructuralSinInferirReglasDeUnaRed(String mti) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("vacio").construir());
        byte[] esperado = ascii(mti + "0000000000000000");
        assertArrayEquals(esperado, codec.codificar(MensajeIso.crear(mti).construir()));
        assertEquals(mti, codec.decodificar(esperado).mti());
    }
    @Test void conservaTodosLosValoresDeByteEnCampoBinario() {
        byte[] contenido = new byte[256];
        for (int indice = 0; indice < contenido.length; indice++) { contenido[indice] = (byte) indice; }
        var codec = new CodecIsoBasico(PerfilMensaje.crear("opaco")
                .campo(DefinicionCampo.lllvar(128, TipoDato.BINARIO, 256)).construir());
        byte[] esperado = unir(ascii("080080000000000000000000000000000001256"), contenido);
        assertArrayEquals(esperado, codec.codificar(MensajeIso.crear("0800").campoBinario(128, contenido).construir()));
        assertArrayEquals(contenido, ((ValorBinario) codec.decodificar(esperado).campo(128)).bytes());
    }
    @ParameterizedTest @EnumSource(FormatoBitmap.class)
    void todosLosCamposSeEscribenEnOrdenYSinConfundirBits(FormatoBitmap formato) {
        var perfil = PerfilMensaje.crear("todos").formatoBitmap(formato);
        var mensaje = MensajeIso.crear("0800");
        byte[] contenido = new byte[127];
        for (int numero = 128; numero >= 2; numero--) {
            perfil.campo(DefinicionCampo.fijo(numero, TipoDato.BINARIO, 1));
            mensaje.campoBinario(numero, new byte[]{(byte) numero});
            contenido[numero - 2] = (byte) numero;
        }
        var codec = new CodecIsoBasico(perfil.construir());
        byte[] esperado = unir(ascii("0800"), bitmap(formato, "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"), contenido);
        var construido = mensaje.construir();
        assertArrayEquals(esperado, codec.codificar(construido));
        assertEquals(construido.campos(), codec.decodificar(esperado).campos());
    }
    @ParameterizedTest @EnumSource(FormatoBitmap.class)
    void secundarioObligatorioRechazaPrimarioSinExtension(FormatoBitmap formato) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("secundario").formatoBitmap(formato)
                .secundarioSiempre(true).construir());
        byte[] esperado = unir(ascii("0800"), bitmap(formato, "80000000000000000000000000000000"));
        assertArrayEquals(esperado, codec.codificar(MensajeIso.crear("0800").construir()));
        assertTrue(codec.decodificar(esperado).campos().isEmpty());
        verificarError(CodigoErrorIso.BITMAP_INVALIDO, null, 4,
                () -> codec.decodificar(unir(ascii("0800"), bitmap(formato, "0000000000000000"))));
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 3})
    void cabeceraConLongitudIncorrectaSeRechazaAntesDeCodificar(int longitud) {
        var codec = new CodecIsoBasico(perfilSobre(FormatoBitmap.BINARIO));
        verificarError(CodigoErrorIso.SOBRE_INVALIDO, null, -1,
                () -> codec.codificar(MensajeIso.crear("0800").cabecera(new byte[longitud]).construir()));
    }
    @ParameterizedTest @EnumSource(FormatoBitmap.class)
    void prefijoOpacoNoSeInterpretaComoTexto(FormatoBitmap formato) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("prefijo-opaco").formatoBitmap(formato)
                .prefijo(new byte[]{0, (byte) 255}).construir());
        byte[] esperado = unir(new byte[]{0, (byte) 255}, ascii("0800"), bitmap(formato, "0000000000000000"));
        assertArrayEquals(esperado, codec.codificar(MensajeIso.crear("0800").construir()));
        assertEquals("0800", codec.decodificar(esperado).mti());
    }
    @Test void conservaCerosSignosYEspaciosSinNormalizarTexto() {
        String imprimibles = IntStream.rangeClosed(32, 126).collect(StringBuilder::new,
                (texto, caracter) -> texto.append((char) caracter), StringBuilder::append).toString();
        var codec = new CodecIsoBasico(PerfilMensaje.crear("texto")
                .campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 95)).construir());
        byte[] esperado = ascii("0200400000000000000095" + imprimibles);
        assertArrayEquals(esperado, codec.codificar(MensajeIso.crear("0200").campo(2, imprimibles).construir()));
        assertEquals(imprimibles, codec.decodificar(esperado).texto(2));
    }
    @ParameterizedTest @EnumSource(TipoDato.class)
    void rechazaTipoLogicoIncompatibleSinConvertirlo(TipoDato tipo) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("tipo")
                .campo(DefinicionCampo.fijo(2, tipo, 3)).construir());
        var mensaje = tipo == TipoDato.BINARIO ? MensajeIso.crear("0200").campo(2, "ABC").construir()
                : MensajeIso.crear("0200").campoBinario(2, ascii("123")).construir();
        verificarError(CodigoErrorIso.CODIFICACION_INVALIDA, 2, -1, () -> codec.codificar(mensaje));
    }
    static Stream<Arguments> caracteresInvalidos() {
        return Stream.of(Arguments.of(TipoDato.NUMERICO_ASCII, " 0"), Arguments.of(TipoDato.NUMERICO_ASCII, "-1"),
                Arguments.of(TipoDato.NUMERICO_ASCII, "1."), Arguments.of(TipoDato.NUMERICO_ASCII, "1A"),
                Arguments.of(TipoDato.TEXTO_ASCII, "A\u0000"), Arguments.of(TipoDato.TEXTO_ASCII, "A\n"),
                Arguments.of(TipoDato.TEXTO_ASCII, "A\u007f"), Arguments.of(TipoDato.TEXTO_ASCII, "Aé"),
                Arguments.of(TipoDato.TEXTO_ASCII, "😀"));
    }
    static Stream<Arguments> bytesEnFronterasAscii() {
        return Stream.of(
                Arguments.of(TipoDato.NUMERICO_ASCII, 47, false), Arguments.of(TipoDato.NUMERICO_ASCII, 48, true),
                Arguments.of(TipoDato.NUMERICO_ASCII, 57, true), Arguments.of(TipoDato.NUMERICO_ASCII, 58, false),
                Arguments.of(TipoDato.NUMERICO_ASCII, 128, false), Arguments.of(TipoDato.NUMERICO_ASCII, 255, false),
                Arguments.of(TipoDato.TEXTO_ASCII, 0, false), Arguments.of(TipoDato.TEXTO_ASCII, 31, false),
                Arguments.of(TipoDato.TEXTO_ASCII, 32, true), Arguments.of(TipoDato.TEXTO_ASCII, 126, true),
                Arguments.of(TipoDato.TEXTO_ASCII, 127, false), Arguments.of(TipoDato.TEXTO_ASCII, 128, false),
                Arguments.of(TipoDato.TEXTO_ASCII, 255, false));
    }
    @ParameterizedTest(name = "{0}, byte={1}, permitido={2}") @MethodSource("bytesEnFronterasAscii")
    void lecturaValidaFronterasAsciiSinAceptarBytesExtendidos(TipoDato tipo, int valor, boolean permitido) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("frontera")
                .campo(DefinicionCampo.fijo(2, tipo, 1)).construir());
        byte[] trama = unir(ascii("02004000000000000000"), new byte[]{(byte) valor});
        if (permitido) {
            assertEquals(Character.toString((char) valor), codec.decodificar(trama).texto(2));
        } else {
            verificarError(CodigoErrorIso.CODIFICACION_INVALIDA, 2, 20, () -> codec.decodificar(trama));
        }
    }
    static Stream<Arguments> prefijosNoAscii() {
        return Stream.of(Arguments.of(TipoLongitud.LLVAR, "02", 0), Arguments.of(TipoLongitud.LLVAR, "02", 255),
                Arguments.of(TipoLongitud.LLLVAR, "002", 0), Arguments.of(TipoLongitud.LLLVAR, "002", 255));
    }
    @ParameterizedTest(name = "{0}: byte {2} en prefijo") @MethodSource("prefijosNoAscii")
    void prefijoDecimalNoAceptaBytesOpacos(TipoLongitud formato, String prefijo, int invalido) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("prefijo")
                .campo(new DefinicionCampo(2, TipoDato.BINARIO, formato, 2)).construir());
        byte[] trama = unir(ascii("02004000000000000000" + prefijo), new byte[]{0, 1});
        trama[20] = (byte) invalido;
        verificarError(CodigoErrorIso.LONGITUD_INVALIDA, 2, 20, () -> codec.decodificar(trama));
    }
    @ParameterizedTest(name = "{0}: caracter invalido, caso {index}") @MethodSource("caracteresInvalidos")
    void rechazaCaracterInvalidoSinSustitucionNiExponerContenido(TipoDato tipo, String texto) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("caracter")
                .campo(DefinicionCampo.fijo(2, tipo, 2)).construir());
        var error = verificarError(CodigoErrorIso.CODIFICACION_INVALIDA, 2, -1,
                () -> codec.codificar(MensajeIso.crear("0200").campo(2, texto).construir()));
        assertFalse(error.getMessage().contains(texto));
    }
    @Test void obligatorioSoloAplicaAlMtiDeclarado() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("obligatorio")
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6)).obligatorios("0200", 11).construir());
        assertArrayEquals(ascii("08000000000000000000"), codec.codificar(MensajeIso.crear("0800").construir()));
        assertEquals("0800", codec.decodificar(ascii("08000000000000000000")).mti());
        verificarError(CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, 11, -1,
                () -> codec.codificar(MensajeIso.crear("0200").construir()));
        verificarError(CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, 11, -1,
                () -> codec.decodificar(ascii("02000000000000000000")));
    }
    @Test void longitudFijaExtremaNoReservaMemoriaSiLaTramaEsCorta() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("extremo")
                .campo(DefinicionCampo.fijo(2, TipoDato.BINARIO, Integer.MAX_VALUE)).construir());
        verificarError(CodigoErrorIso.TRAMA_INCOMPLETA, 2, 20,
                () -> codec.decodificar(ascii("02004000000000000000")));
    }
    @Test void rechazaArgumentosNulosDelCodec() {
        assertThrows(NullPointerException.class, () -> new CodecIsoBasico(null));
        var codec = new CodecIsoBasico(PerfilMensaje.crear("nulos").construir());
        assertThrows(NullPointerException.class, () -> codec.codificar(null));
        assertThrows(NullPointerException.class, () -> codec.decodificar(null));
    }
}
