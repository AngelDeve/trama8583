package org.gca.trama8583.nucleo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Perfil inmutable del subconjunto implementado; no implica certificacion de una red. */
public final class PerfilMensaje {
    private final String identificador;
    private final Map<Integer, DefinicionCampo> campos;
    private final Map<String, Set<Integer>> obligatorios;
    private final FormatoBitmap formatoBitmap;
    private final boolean secundarioSiempre;
    private final byte[] prefijo;
    private final int longitudCabecera;
    private final int maximoBytes;

    private PerfilMensaje(Constructor constructor) {
        identificador = constructor.identificador;
        campos = Collections.unmodifiableMap(new TreeMap<>(constructor.campos));
        Map<String, Set<Integer>> copia = new HashMap<>();
        constructor.obligatorios.forEach((mti, numeros) -> {
            MensajeIso.validarMti(mti);
            if (!campos.keySet().containsAll(numeros)) {
                throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, null, -1,
                        "Los campos obligatorios deben estar definidos en el perfil");
            }
            copia.put(mti, Set.copyOf(numeros));
        });
        obligatorios = Map.copyOf(copia);
        formatoBitmap = constructor.formatoBitmap;
        secundarioSiempre = constructor.secundarioSiempre;
        prefijo = constructor.prefijo.clone();
        longitudCabecera = constructor.longitudCabecera;
        maximoBytes = constructor.maximoBytes;
        int longitudBitmap = formatoBitmap == FormatoBitmap.BINARIO ? 8 : 16;
        long minimo = (long) prefijo.length + longitudCabecera + 4 + longitudBitmap * (secundarioSiempre ? 2 : 1);
        if (longitudCabecera < 0 || maximoBytes < minimo) {
            throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, null, -1,
                    "Los limites del perfil no permiten representar el sobre y el bitmap");
        }
    }

    public static Constructor crear(String identificador) { return new Constructor(identificador); }
    public String identificador() { return identificador; }
    public Map<Integer, DefinicionCampo> campos() { return campos; }
    public Set<Integer> obligatorios(String mti) { return obligatorios.getOrDefault(mti, Set.of()); }
    public FormatoBitmap formatoBitmap() { return formatoBitmap; }
    public boolean secundarioSiempre() { return secundarioSiempre; }
    public byte[] prefijo() { return prefijo.clone(); }
    public int longitudCabecera() { return longitudCabecera; }
    public int maximoBytes() { return maximoBytes; }

    /** Constructor del perfil, sin carga remota, reflexion ni dependencia JSON. */
    public static final class Constructor {
        private final String identificador;
        private final Map<Integer, DefinicionCampo> campos = new TreeMap<>();
        private final Map<String, Set<Integer>> obligatorios = new HashMap<>();
        private FormatoBitmap formatoBitmap = FormatoBitmap.HEXADECIMAL_ASCII;
        private boolean secundarioSiempre;
        private byte[] prefijo = new byte[0];
        private int longitudCabecera;
        private int maximoBytes = 4096;
        private Constructor(String identificador) {
            this.identificador = Objects.requireNonNull(identificador, "El identificador es obligatorio");
            if (identificador.isBlank()) {
                throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, null, -1,
                        "El identificador del perfil no puede estar vacio");
            }
        }
        public Constructor campo(DefinicionCampo campo) {
            Objects.requireNonNull(campo, "La definicion no puede ser nula");
            if (campos.putIfAbsent(campo.numero(), campo) != null) {
                throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, campo.numero(), -1,
                        "El campo ya esta definido; utilice reemplazarCampo para una variacion explicita");
            }
            return this;
        }
        public Constructor reemplazarCampo(DefinicionCampo campo) {
            Objects.requireNonNull(campo, "La definicion no puede ser nula");
            campos.put(campo.numero(), campo);
            return this;
        }
        public Constructor obligatorios(String mti, int... numeros) {
            MensajeIso.validarMti(mti);
            Set<Integer> conjunto = new HashSet<>();
            for (int numero : numeros) { MensajeIso.validarNumeroCampo(numero); conjunto.add(numero); }
            obligatorios.put(mti, conjunto);
            return this;
        }
        public Constructor formatoBitmap(FormatoBitmap formato) {
            formatoBitmap = Objects.requireNonNull(formato, "El formato del bitmap es obligatorio");
            return this;
        }
        public Constructor secundarioSiempre(boolean valor) { secundarioSiempre = valor; return this; }
        public Constructor prefijo(byte[] bytes) {
            prefijo = Objects.requireNonNull(bytes, "El prefijo no puede ser nulo").clone();
            return this;
        }
        public Constructor prefijoAscii(String texto) {
            Objects.requireNonNull(texto, "El prefijo no puede ser nulo");
            if (texto.chars().anyMatch(caracter -> caracter > 127)) {
                throw new ExcepcionIso(CodigoErrorIso.PERFIL_INVALIDO, null, -1,
                        "El prefijo ASCII contiene caracteres no representables");
            }
            return prefijo(texto.getBytes(StandardCharsets.US_ASCII));
        }
        public Constructor longitudCabecera(int longitud) { longitudCabecera = longitud; return this; }
        public Constructor maximoBytes(int maximo) { maximoBytes = maximo; return this; }
        public PerfilMensaje construir() { return new PerfilMensaje(this); }
    }
}
