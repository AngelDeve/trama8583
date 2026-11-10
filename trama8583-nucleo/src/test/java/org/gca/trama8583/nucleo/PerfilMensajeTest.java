package org.gca.trama8583.nucleo;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PerfilMensajeTest {
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t\n"})
    void rechazaIdentificadorAusente(String identificador) {
        if (identificador == null) { assertThrows(NullPointerException.class, () -> PerfilMensaje.crear(null)); }
        else { assertEquals(CodigoErrorIso.PERFIL_INVALIDO,
                assertThrows(ExcepcionIso.class, () -> PerfilMensaje.crear(identificador)).codigo()); }
    }
    static Stream<Arguments> longitudesDeDefinicionInvalidas() {
        return Stream.of(Arguments.of(TipoLongitud.FIJA, -1), Arguments.of(TipoLongitud.FIJA, 0),
                Arguments.of(TipoLongitud.LLVAR, -1), Arguments.of(TipoLongitud.LLVAR, 0),
                Arguments.of(TipoLongitud.LLVAR, 100), Arguments.of(TipoLongitud.LLLVAR, -1),
                Arguments.of(TipoLongitud.LLLVAR, 0), Arguments.of(TipoLongitud.LLLVAR, 1000));
    }
    @ParameterizedTest(name = "{0}: definicion con longitud invalida {1}")
    @MethodSource("longitudesDeDefinicionInvalidas")
    void rechazaLongitudNoRepresentable(TipoLongitud formato, int longitud) {
        var error = assertThrows(ExcepcionIso.class,
                () -> new DefinicionCampo(48, TipoDato.BINARIO, formato, longitud));
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, error.codigo());
        assertEquals(48, error.campo());
        assertEquals(-1, error.posicion());
    }
    @ParameterizedTest @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1, 129, Integer.MAX_VALUE})
    void rechazaCampoFueraDeRangoEnDefinicionYObligatorios(int numero) {
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> DefinicionCampo.fijo(numero, TipoDato.BINARIO, 1)).codigo());
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> PerfilMensaje.crear("rango").obligatorios("0200", numero)).codigo());
    }
    static Stream<Arguments> limitesDelSobre() {
        return Stream.of(Arguments.of(FormatoBitmap.HEXADECIMAL_ASCII, false, 20),
                Arguments.of(FormatoBitmap.HEXADECIMAL_ASCII, true, 36),
                Arguments.of(FormatoBitmap.BINARIO, false, 12), Arguments.of(FormatoBitmap.BINARIO, true, 20));
    }
    @ParameterizedTest(name = "{0}, secundario={1}: minimo={2} sin sobre") @MethodSource("limitesDelSobre")
    void limiteMinimoIncluyeBitmapPrefijoYCabecera(FormatoBitmap formato, boolean secundario, int minimo) {
        var constructor = PerfilMensaje.crear("minimo").formatoBitmap(formato).secundarioSiempre(secundario)
                .prefijoAscii("ISO").longitudCabecera(2).maximoBytes(minimo + 5);
        assertEquals(minimo + 5, constructor.construir().maximoBytes());
        var error = assertThrows(ExcepcionIso.class, () -> constructor.maximoBytes(minimo + 4).construir());
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, error.codigo());
    }
    @ParameterizedTest @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 11})
    void rechazaLimiteTotalInsuficiente(int limite) {
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> PerfilMensaje.crear("limite").maximoBytes(limite).construir()).codigo());
    }
    @ParameterizedTest @ValueSource(ints = {-1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rechazaCabeceraNegativaOSobreQueDesbordaElLimite(int longitud) {
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> PerfilMensaje.crear("cabecera").longitudCabecera(longitud).maximoBytes(Integer.MAX_VALUE).construir()).codigo());
    }
    @ParameterizedTest @ValueSource(strings = {"ISÓ", "\u0080", "😀"})
    void prefijoAsciiNoReemplazaCaracteres(String prefijo) {
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> PerfilMensaje.crear("prefijo").prefijoAscii(prefijo)).codigo());
    }
    @Test void duplicadosExigenReemplazoExplicito() {
        var constructor = PerfilMensaje.crear("duplicado")
                .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8));
        var original = constructor.construir();
        var error = assertThrows(ExcepcionIso.class,
                () -> constructor.campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 16)));
        assertEquals(41, error.campo());
        assertEquals(8, constructor.construir().campos().get(41).longitudMaxima());
        var variante = constructor.reemplazarCampo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 16)).construir();
        assertEquals(16, variante.campos().get(41).longitudMaxima());
        assertEquals(8, original.campos().get(41).longitudMaxima());
    }
    @Test void obligatoriosSonPorMtiYSinAliasDelArreglo() {
        int[] campos = {11, 11};
        var constructor = PerfilMensaje.crear("obligatorios")
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                .campo(DefinicionCampo.fijo(39, TipoDato.TEXTO_ASCII, 2))
                .obligatorios("0200", campos).obligatorios("0210", 11, 39);
        campos[0] = 39;
        var perfil = constructor.construir();
        assertEquals(Set.of(11), perfil.obligatorios("0200"));
        assertEquals(Set.of(11, 39), perfil.obligatorios("0210"));
        assertEquals(Set.of(), perfil.obligatorios("0800"));
        constructor.obligatorios("0200", 39);
        assertEquals(Set.of(11), perfil.obligatorios("0200"));
        assertEquals(Set.of(39), constructor.construir().obligatorios("0200"));
    }
    @Test void noPermiteObligatorioSinDefinicionNiMtiInvalido() {
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> PerfilMensaje.crear("ausente").obligatorios("0200", 11).construir()).codigo());
        assertEquals(CodigoErrorIso.MTI_INVALIDO, assertThrows(ExcepcionIso.class,
                () -> PerfilMensaje.crear("mti").obligatorios("ABC", 11)).codigo());
    }
    @Test void prefijoOpacoYOrdenDelPerfilSeConservan() {
        byte[] prefijo = {0, (byte) 255};
        var perfil = PerfilMensaje.crear("opaco").prefijo(prefijo)
                .campo(DefinicionCampo.fijo(128, TipoDato.BINARIO, 1))
                .campo(DefinicionCampo.fijo(2, TipoDato.BINARIO, 1)).construir();
        prefijo[0] = 9; perfil.prefijo()[1] = 0;
        assertArrayEquals(new byte[]{0, (byte) 255}, perfil.prefijo());
        assertEquals(List.of(2, 128), List.copyOf(perfil.campos().keySet()));
    }
    static Stream<Arguments> parametrosNulos() {
        return Stream.of(
                Arguments.of("campo", (Consumer<PerfilMensaje.Constructor>) perfil -> perfil.campo(null)),
                Arguments.of("reemplazo", (Consumer<PerfilMensaje.Constructor>) perfil -> perfil.reemplazarCampo(null)),
                Arguments.of("bitmap", (Consumer<PerfilMensaje.Constructor>) perfil -> perfil.formatoBitmap(null)),
                Arguments.of("prefijo binario", (Consumer<PerfilMensaje.Constructor>) perfil -> perfil.prefijo(null)),
                Arguments.of("prefijo ASCII", (Consumer<PerfilMensaje.Constructor>) perfil -> perfil.prefijoAscii(null)));
    }
    @ParameterizedTest(name = "rechaza {0} nulo") @MethodSource("parametrosNulos")
    void rechazaParametrosNulos(String escenario, Consumer<PerfilMensaje.Constructor> accion) {
        assertThrows(NullPointerException.class, () -> accion.accept(PerfilMensaje.crear("nulos")));
    }
    @Test void definicionExigeTiposNoNulos() {
        assertThrows(NullPointerException.class, () -> new DefinicionCampo(2, null, TipoLongitud.FIJA, 1));
        assertThrows(NullPointerException.class, () -> new DefinicionCampo(2, TipoDato.BINARIO, null, 1));
    }
}
