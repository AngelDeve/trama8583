package org.gca.trama8583.nucleo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModeloIsoTest {
    @Test void mensajeYCabeceraSonInstantaneasDefensivas() {
        byte[] datos = {1, 2};
        var constructor = MensajeIso.crear("0200").cabecera(datos).campoBinario(48, datos);
        var mensaje = constructor.construir();
        datos[0] = 9;
        constructor.mti("0210").campo(11, "000001");
        mensaje.cabecera()[0] = 8;
        ((ValorBinario) mensaje.campo(48)).bytes()[0] = 7;
        assertArrayEquals(new byte[]{1, 2}, mensaje.cabecera());
        assertArrayEquals(new byte[]{1, 2}, ((ValorBinario) mensaje.campo(48)).bytes());
        assertEquals("0200", mensaje.mti());
        assertNull(mensaje.campo(11));
        assertThrows(UnsupportedOperationException.class, () -> mensaje.campos().clear());
        assertEquals("0210", mensaje.copiar().mti("0210").construir().mti());
    }
    @Test void perfilEsInmutableYRechazaDefinicionesAmbiguas() {
        byte[] prefijo = {1};
        var campo = DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6);
        var constructor = PerfilMensaje.crear("base").prefijo(prefijo).campo(campo).obligatorios("0200", 11);
        var perfil = constructor.construir();
        prefijo[0] = 2; perfil.prefijo()[0] = 3;
        constructor.reemplazarCampo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 8));
        assertArrayEquals(new byte[]{1}, perfil.prefijo());
        assertEquals(6, perfil.campos().get(11).longitudMaxima());
        assertThrows(UnsupportedOperationException.class, () -> perfil.campos().clear());
        assertThrows(UnsupportedOperationException.class, () -> perfil.obligatorios("0200").clear());
        assertThrows(ExcepcionIso.class, () -> constructor.campo(campo));
        assertThrows(ExcepcionIso.class, () -> PerfilMensaje.crear("invalido").obligatorios("0200", 11).construir());
        assertThrows(ExcepcionIso.class, () -> DefinicionCampo.llvar(2, TipoDato.TEXTO_ASCII, 100));
    }
    @Test void restringeMtiYCamposFueraDelSubconjunto() {
        for (String mti : new String[]{"200", "02000", "02A0", "０２００"}) {
            assertThrows(ExcepcionIso.class, () -> MensajeIso.crear(mti).construir());
        }
        for (int numero : new int[]{0, 1, 129}) {
            assertThrows(ExcepcionIso.class, () -> MensajeIso.crear("0200").campo(numero, "X"));
        }
    }
}
