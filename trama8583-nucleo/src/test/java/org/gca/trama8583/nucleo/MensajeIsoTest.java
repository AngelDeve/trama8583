package org.gca.trama8583.nucleo;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MensajeIsoTest {
    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"200", "02000", "02A0", "０２００", " 200", "020\n"})
    void rechazaMtiInvalidoAlConstruirYCopiar(String mti) {
        var error = assertThrows(ExcepcionIso.class, () -> MensajeIso.crear(mti).construir());
        assertEquals(CodigoErrorIso.MTI_INVALIDO, error.codigo());
        assertNull(error.campo());
        assertEquals(-1, error.posicion());
        assertThrows(ExcepcionIso.class, () -> MensajeIso.crear("0200").construir().copiar().mti(mti).construir());
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0, 1, 129})
    void rechazaCamposFueraDeRangoSinGuardarValores(int numero) {
        var constructor = MensajeIso.crear("0200");
        var error = assertThrows(ExcepcionIso.class, () -> constructor.campo(numero, "VALOR"));
        assertEquals(CodigoErrorIso.PERFIL_INVALIDO, error.codigo());
        assertEquals(numero, error.campo());
        assertTrue(constructor.construir().campos().isEmpty());
    }
    @Test void copiarAgregarReemplazarYQuitarNoModificanElOriginal() {
        var original = MensajeIso.crear("0200").campo(11, "000001").campo(41, "TERM0001").construir();
        var constructor = original.copiar().mti("0210").campo(11, "000002").campo(39, "00").quitarCampo(41);
        var respuesta = constructor.construir();
        constructor.quitarCampo(11).campo(39, "05");
        assertEquals("0200", original.mti());
        assertEquals("000001", original.texto(11));
        assertEquals("TERM0001", original.texto(41));
        assertNull(original.campo(39));
        assertEquals("0210", respuesta.mti());
        assertEquals("000002", respuesta.texto(11));
        assertEquals("00", respuesta.texto(39));
        assertNull(respuesta.campo(41));
        assertEquals(List.of(11, 39), List.copyOf(respuesta.campos().keySet()));
    }
    @Test void accederTextoAusenteOBinarioNoConvierteDatos() {
        var mensaje = MensajeIso.crear("0200").campoBinario(48, new byte[]{1}).construir();
        assertThrows(IllegalArgumentException.class, () -> mensaje.texto(2));
        assertThrows(IllegalArgumentException.class, () -> mensaje.texto(48));
        assertNull(mensaje.campo(2));
    }
    @Test void valoresConservanContenidoEIgualdadSinRevelarlo() {
        String secreto = "0001 - secreto ";
        var texto = new ValorTexto(secreto);
        assertEquals(secreto, texto.texto());
        assertEquals(texto, new ValorTexto(secreto));
        assertFalse(texto.toString().contains(secreto));
        var binario = new ValorBinario(new byte[]{0, (byte) 255});
        var igual = new ValorBinario(new byte[]{0, (byte) 255});
        assertEquals(binario, igual);
        assertEquals(binario.hashCode(), igual.hashCode());
        assertNotEquals(binario, new ValorBinario(new byte[]{0}));
        assertNotEquals(binario, texto);
        assertNotEquals(binario, null);
        assertFalse(binario.toString().contains("255"));
        var mensaje = MensajeIso.crear("0200").cabecera(new byte[]{9, 8}).campo(2, texto).campo(48, binario).construir();
        assertFalse(mensaje.toString().contains(secreto));
        assertFalse(mensaje.toString().contains("[9, 8]"));
    }
    @Test void rechazaNulosEnValoresYCabecera() {
        assertThrows(NullPointerException.class, () -> new ValorTexto(null));
        assertThrows(NullPointerException.class, () -> new ValorBinario(null));
        var constructor = MensajeIso.crear("0200");
        assertThrows(NullPointerException.class, () -> constructor.cabecera(null));
        assertThrows(NullPointerException.class, () -> constructor.campo(2, (ValorCampo) null));
        assertThrows(NullPointerException.class, () -> constructor.campo(2, (String) null));
        assertThrows(NullPointerException.class, () -> constructor.campoBinario(2, null));
    }
}
