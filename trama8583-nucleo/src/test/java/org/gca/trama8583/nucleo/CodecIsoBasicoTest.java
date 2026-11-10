package org.gca.trama8583.nucleo;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CodecIsoBasicoTest {
    // Vectores sinteticos documentados en examples/messages/vectors.json.
    private static final String PRIMARIO = "02002020000000800000000000000001TERM0001";
    private static byte[] ascii(String texto) { return texto.getBytes(StandardCharsets.US_ASCII); }
    private static PerfilMensaje.Constructor base() {
        return PerfilMensaje.crear("prueba")
                .campo(DefinicionCampo.fijo(3, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8));
    }
    private static MensajeIso solicitud() {
        return MensajeIso.crear("0200").campo(41, "TERM0001")
                .campo(11, "000001").campo(3, "000000").construir();
    }
    private static void comprobar(CodecIso codec, MensajeIso mensaje, byte[] esperado) {
        assertArrayEquals(esperado, codec.codificar(mensaje));
        MensajeIso resultado = codec.decodificar(esperado);
        assertEquals(mensaje.mti(), resultado.mti());
        assertEquals(mensaje.campos(), resultado.campos());
        assertArrayEquals(mensaje.cabecera(), resultado.cabecera());
    }
    @Test void respetaVectorPrimarioYOrdenAscendente() {
        comprobar(new CodecIsoBasico(base().construir()), solicitud(), ascii(PRIMARIO));
    }
    @Test void permiteVariantesExplicitasDeLongitud() {
        var perfil = base().reemplazarCampo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 16))
                .campo(DefinicionCampo.fijo(54, TipoDato.NUMERICO_ASCII, 12)).construir();
        var mensaje = solicitud().copiar().campo(41, "TERM000000000001").campo(54, "000000000100").construir();
        comprobar(new CodecIsoBasico(perfil), mensaje,
                ascii("0200" + "2020000000800400" + "000000000001TERM000000000001000000000100"));
    }
    @Test void respetaVectorSecundario() {
        var perfil = base().campo(DefinicionCampo.fijo(70, TipoDato.NUMERICO_ASCII, 3)).construir();
        var mensaje = MensajeIso.crear("0800").campo(11, "000002").campo(70, "301").construir();
        comprobar(new CodecIsoBasico(perfil), mensaje,
                ascii("0800" + "80200000000000000400000000000000" + "000002301"));
    }
    @Test void combinaBitmapBinarioSobreYLongitudesVariablesEnBytes() {
        var perfil = PerfilMensaje.crear("binario").formatoBitmap(FormatoBitmap.BINARIO)
                .prefijoAscii("ISO").longitudCabecera(2)
                .campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 99))
                .campo(DefinicionCampo.lllvar(48, TipoDato.BINARIO, 999)).construir();
        var mensaje = MensajeIso.crear("0200").cabecera(new byte[]{1, 2})
                .campo(2, "ABC").campoBinario(48, new byte[]{0, (byte) 255}).construir();
        comprobar(new CodecIsoBasico(perfil), mensaje, HexFormat.of().parseHex(
                "49534F0102303230304000000000010000303341424330303200FF"));
    }
    @ParameterizedTest @ValueSource(ints = {2, 64, 65, 128})
    void respetaExtremosDelBitmap(int numero) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("extremos")
                .formatoBitmap(FormatoBitmap.BINARIO)
                .campo(DefinicionCampo.fijo(numero, TipoDato.BINARIO, 1)).construir());
        byte[] esperado = new byte[numero > 64 ? 21 : 13];
        System.arraycopy(ascii("0800"), 0, esperado, 0, 4);
        if (numero > 64) { esperado[4] = (byte) 128; }
        esperado[4 + (numero - 1) / 8] |= (byte) (128 >>> ((numero - 1) % 8));
        esperado[esperado.length - 1] = 42;
        comprobar(codec, MensajeIso.crear("0800").campoBinario(numero, new byte[]{42}).construir(), esperado);
    }
    @Test void admiteSecundarioVacioCuandoElPerfilLoExige() {
        var codec = new CodecIsoBasico(base().secundarioSiempre(true).construir());
        comprobar(codec, MensajeIso.crear("0800").construir(), ascii("080080000000000000000000000000000000"));
        assertEquals(CodigoErrorIso.BITMAP_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii(PRIMARIO))).codigo());
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 99})
    void admiteLimitesLlvar(int longitud) {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("llvar")
                .campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 99)).construir());
        var mensaje = MensajeIso.crear("0200").campo(2, "A".repeat(longitud)).construir();
        comprobar(codec, mensaje, ascii("02004000000000000000" + String.format(Locale.ROOT, "%02d", longitud) + "A".repeat(longitud)));
    }
    @Test void admiteMaximoLllvar() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("lllvar")
                .campo(DefinicionCampo.lllvar(48, TipoDato.TEXTO_ASCII, 999)).construir());
        comprobar(codec, MensajeIso.crear("0200").campo(48, "X".repeat(999)).construir(),
                ascii("02000000000000010000999" + "X".repeat(999)));
    }
    @Test void rechazaCadaTruncamientoDelVector() {
        var codec = new CodecIsoBasico(base().construir());
        byte[] trama = ascii(PRIMARIO);
        for (int longitud = 0; longitud < trama.length; longitud++) {
            byte[] truncada = Arrays.copyOf(trama, longitud);
            var error = assertThrows(ExcepcionIso.class, () -> codec.decodificar(truncada));
            assertEquals(CodigoErrorIso.TRAMA_INCOMPLETA, error.codigo());
            assertTrue(error.posicion() >= 0 && error.posicion() <= longitud);
        }
    }
    @Test void rechazaDatosSobrantesCampoDesconocidoMtiYBitmapInvalidos() {
        var codec = new CodecIsoBasico(base().construir());
        assertEquals(CodigoErrorIso.DATOS_SOBRANTES, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii(PRIMARIO + "X"))).codigo());
        assertEquals(CodigoErrorIso.CAMPO_NO_DEFINIDO, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii("02004000000000000000"))).codigo());
        assertEquals(CodigoErrorIso.MTI_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii("A2000000000000000000"))).codigo());
        assertEquals(CodigoErrorIso.BITMAP_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii("0200G000000000000000"))).codigo());
    }
    @Test void rechazaPrefijosVariablesInvalidosAntesDeLeerContenido() {
        var codec = new CodecIsoBasico(PerfilMensaje.crear("variable")
                .campo(DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 5)).construir());
        for (String prefijo : new String[]{"A1", "06"}) {
            var error = assertThrows(ExcepcionIso.class,
                    () -> codec.decodificar(ascii("02004000000000000000" + prefijo)));
            assertEquals(CodigoErrorIso.LONGITUD_INVALIDA, error.codigo());
            assertEquals(2, error.campo());
            assertEquals(20, error.posicion());
        }
    }
    @Test void validaObligatoriosTipoLongitudYCodificacionSinRevelarContenido() {
        var codec = new CodecIsoBasico(base().obligatorios("0200", 3, 11, 41).construir());
        assertEquals(CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, assertThrows(ExcepcionIso.class,
                () -> codec.codificar(MensajeIso.crear("0200").construir())).codigo());
        assertEquals(CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii("02000000000000000000"))).codigo());
        assertEquals(CodigoErrorIso.LONGITUD_INVALIDA, assertThrows(ExcepcionIso.class,
                () -> codec.codificar(solicitud().copiar().campo(3, "1").construir())).codigo());
        for (String valor : new String[]{"ABCDEF", "TÉRM0001", "TERM000\n"}) {
            var mensaje = solicitud().copiar().campo(valor.equals("ABCDEF") ? 3 : 41, valor).construir();
            var error = assertThrows(ExcepcionIso.class, () -> codec.codificar(mensaje));
            assertEquals(CodigoErrorIso.CODIFICACION_INVALIDA, error.codigo());
            assertFalse(error.getMessage().contains(valor));
            assertFalse(mensaje.toString().contains(valor));
        }
        assertEquals(CodigoErrorIso.CODIFICACION_INVALIDA, assertThrows(ExcepcionIso.class,
                () -> codec.codificar(solicitud().copiar().campoBinario(3, new byte[6]).construir())).codigo());
        byte[] trama = ascii(PRIMARIO); trama[20] = (byte) 255;
        var error = assertThrows(ExcepcionIso.class, () -> codec.decodificar(trama));
        assertEquals(CodigoErrorIso.CODIFICACION_INVALIDA, error.codigo());
        assertEquals(20, error.posicion());
    }
    @Test void limitaMensajeYSobreSecundario() {
        var codec = new CodecIsoBasico(base().maximoBytes(39).construir());
        assertEquals(CodigoErrorIso.LIMITE_EXCEDIDO, assertThrows(ExcepcionIso.class,
                () -> codec.codificar(solicitud())).codigo());
        assertEquals(CodigoErrorIso.LIMITE_EXCEDIDO, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii(PRIMARIO))).codigo());
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> base().secundarioSiempre(true).maximoBytes(20).construir()).codigo());
        var secundario = new CodecIsoBasico(base().maximoBytes(20)
                .campo(DefinicionCampo.fijo(70, TipoDato.NUMERICO_ASCII, 3)).construir());
        assertEquals(CodigoErrorIso.LIMITE_EXCEDIDO, assertThrows(ExcepcionIso.class,
                () -> secundario.codificar(MensajeIso.crear("0800").campo(70, "301").construir())).codigo());
    }
    @Test void validaSobre() {
        var codec = new CodecIsoBasico(base().prefijoAscii("ISO").longitudCabecera(2).construir());
        assertEquals(CodigoErrorIso.SOBRE_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> codec.codificar(solicitud())).codigo());
        assertEquals(CodigoErrorIso.SOBRE_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> codec.decodificar(ascii(PRIMARIO))).codigo());
    }
    @Test void entradasAleatoriasNoProvocanErroresDeIndices() {
        var codec = new CodecIsoBasico(base().construir());
        var aleatorio = new Random(8583);
        for (int indice = 0; indice < 1000; indice++) {
            byte[] trama = new byte[aleatorio.nextInt(100)]; aleatorio.nextBytes(trama);
            try { codec.decodificar(trama); } catch (ExcepcionIso error) {
                assertNotNull(error.codigo());
            }
        }
    }
}
