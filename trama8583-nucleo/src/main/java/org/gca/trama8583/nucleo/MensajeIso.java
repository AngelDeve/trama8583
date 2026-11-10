package org.gca.trama8583.nucleo;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Mensaje ISO independiente de Spring y de un perfil de codificacion. */
public final class MensajeIso {
    private final String mti;
    private final Map<Integer, ValorCampo> campos;
    private final byte[] cabecera;

    private MensajeIso(Constructor constructor) {
        validarMti(constructor.mti);
        mti = constructor.mti;
        campos = Collections.unmodifiableMap(new TreeMap<>(constructor.campos));
        cabecera = constructor.cabecera.clone();
    }

    public static Constructor crear(String mti) { return new Constructor(mti); }
    public Constructor copiar() {
        Constructor constructor = new Constructor(mti);
        constructor.campos.putAll(campos);
        constructor.cabecera = cabecera.clone();
        return constructor;
    }
    public String mti() { return mti; }
    public Map<Integer, ValorCampo> campos() { return campos; }
    public byte[] cabecera() { return cabecera.clone(); }
    public ValorCampo campo(int numero) { return campos.get(numero); }
    public String texto(int numero) {
        if (campos.get(numero) instanceof ValorTexto valor) { return valor.texto(); }
        throw new IllegalArgumentException("El campo solicitado no contiene texto");
    }
    static void validarMti(String mti) {
        if (mti == null || !mti.matches("[0-9]{4}")) {
            throw new ExcepcionIso(CodigoErrorIso.MTI_INVALIDO, null, -1,
                    "El MTI debe contener cuatro digitos ASCII");
        }
    }
    static void validarNumeroCampo(int numero) {
        if (numero < 2 || numero > 128) {
            throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, numero, -1,
                    "Los campos de datos deben estar entre 2 y 128; el bitmap se genera automaticamente");
        }
    }
    @Override public String toString() {
        return "MensajeIso[mti=" + mti + ", campos=" + campos.keySet() + ", valores omitidos]";
    }

    /** Constructor reutilizable; cada construir produce una instantanea inmutable. */
    public static final class Constructor {
        private String mti;
        private final Map<Integer, ValorCampo> campos = new TreeMap<>();
        private byte[] cabecera = new byte[0];
        private Constructor(String mti) { this.mti = mti; }
        public Constructor mti(String mti) { this.mti = mti; return this; }
        public Constructor campo(int numero, String texto) { return campo(numero, new ValorTexto(texto)); }
        public Constructor campoBinario(int numero, byte[] bytes) { return campo(numero, new ValorBinario(bytes)); }
        public Constructor campo(int numero, ValorCampo valor) {
            validarNumeroCampo(numero);
            campos.put(numero, Objects.requireNonNull(valor, "El valor no puede ser nulo"));
            return this;
        }
        public Constructor quitarCampo(int numero) { campos.remove(numero); return this; }
        public Constructor cabecera(byte[] bytes) {
            cabecera = Objects.requireNonNull(bytes, "La cabecera no puede ser nula").clone();
            return this;
        }
        public MensajeIso construir() { return new MensajeIso(this); }
    }
}
