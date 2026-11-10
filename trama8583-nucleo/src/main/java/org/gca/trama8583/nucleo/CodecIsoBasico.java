package org.gca.trama8583.nucleo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Codec estricto de MTI ASCII, bitmap 64/128 y prefijos ASCII medidos en bytes. */
public final class CodecIsoBasico implements CodecIso {
    private static final char[] HEXADECIMALES = "0123456789ABCDEF".toCharArray();
    private final PerfilMensaje perfil;

    public CodecIsoBasico(PerfilMensaje perfil) {
        this.perfil = Objects.requireNonNull(perfil, "El perfil es obligatorio");
    }

    @Override public byte[] codificar(MensajeIso mensaje) {
        Objects.requireNonNull(mensaje, "El mensaje es obligatorio");
        validarObligatorios(mensaje);
        byte[] cabecera = mensaje.cabecera();
        if (cabecera.length != perfil.longitudCabecera()) {
            throw error(CodigoErrorIso.SOBRE_INVALIDO, null, -1,
                    "La longitud de cabecera no coincide con el perfil");
        }
        boolean secundario = perfil.secundarioSiempre() || mensaje.campos().keySet().stream().anyMatch(numero -> numero > 64);
        byte[] bitmap = new byte[secundario ? 16 : 8];
        if (secundario) { activarBit(bitmap, 1); }
        Map<Integer, byte[]> contenidos = new TreeMap<>();
        long longitud = (long) perfil.prefijo().length + cabecera.length + 4
                + bitmap.length * (perfil.formatoBitmap() == FormatoBitmap.BINARIO ? 1 : 2);
        if (longitud > perfil.maximoBytes()) {
            throw error(CodigoErrorIso.LIMITE_EXCEDIDO, null, -1, "El sobre supera el limite de bytes del perfil");
        }
        for (var entrada : mensaje.campos().entrySet()) {
            int numero = entrada.getKey();
            DefinicionCampo campo = definicion(numero, -1);
            int longitudValor = entrada.getValue() instanceof ValorTexto texto
                    ? texto.texto().length() : ((ValorBinario) entrada.getValue()).longitud();
            validarLongitud(campo, longitudValor, -1);
            longitud += campo.tipoLongitud().digitosPrefijo() + (long) longitudValor;
            if (longitud > perfil.maximoBytes()) {
                throw error(CodigoErrorIso.LIMITE_EXCEDIDO, numero, -1, "El mensaje supera el limite de bytes del perfil");
            }
            byte[] contenido = codificarValor(entrada.getValue(), campo);
            contenidos.put(numero, contenido);
            activarBit(bitmap, numero);
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream((int) longitud);
        salida.writeBytes(perfil.prefijo());
        salida.writeBytes(cabecera);
        salida.writeBytes(mensaje.mti().getBytes(StandardCharsets.US_ASCII));
        escribirBitmap(salida, bitmap);
        contenidos.forEach((numero, contenido) -> {
            int digitos = perfil.campos().get(numero).tipoLongitud().digitosPrefijo();
            if (digitos > 0) {
                String longitudTexto = Integer.toString(contenido.length);
                salida.writeBytes(("0".repeat(digitos - longitudTexto.length()) + longitudTexto)
                        .getBytes(StandardCharsets.US_ASCII));
            }
            salida.writeBytes(contenido);
        });
        return salida.toByteArray();
    }

    @Override public MensajeIso decodificar(byte[] datos) {
        Objects.requireNonNull(datos, "La trama es obligatoria");
        if (datos.length > perfil.maximoBytes()) {
            throw error(CodigoErrorIso.LIMITE_EXCEDIDO, null, 0, "La trama supera el limite de bytes del perfil");
        }
        Cursor cursor = new Cursor(datos.clone());
        byte[] prefijo = perfil.prefijo();
        if (!Arrays.equals(prefijo, cursor.leer(prefijo.length, null))) {
            throw error(CodigoErrorIso.SOBRE_INVALIDO, null, 0, "El prefijo no coincide con el perfil");
        }
        byte[] cabecera = cursor.leer(perfil.longitudCabecera(), null);
        int posicionMti = cursor.posicion;
        byte[] bytesMti = cursor.leer(4, null);
        for (byte valor : bytesMti) {
            if (valor < '0' || valor > '9') {
                throw error(CodigoErrorIso.MTI_INVALIDO, null, posicionMti, "El MTI debe contener cuatro digitos ASCII");
            }
        }
        String mti = new String(bytesMti, StandardCharsets.US_ASCII);
        byte[] primario = leerBitmap(cursor);
        boolean secundario = bitActivo(primario, 1);
        if (perfil.secundarioSiempre() && !secundario) {
            throw error(CodigoErrorIso.BITMAP_INVALIDO, null, posicionMti + 4,
                    "El perfil requiere un bitmap secundario");
        }
        byte[] bitmap = Arrays.copyOf(primario, secundario ? 16 : 8);
        if (secundario) { System.arraycopy(leerBitmap(cursor), 0, bitmap, 8, 8); }
        MensajeIso.Constructor constructor = MensajeIso.crear(mti).cabecera(cabecera);
        for (int numero = 2; numero <= bitmap.length * 8; numero++) {
            if (!bitActivo(bitmap, numero)) { continue; }
            DefinicionCampo campo = definicion(numero, cursor.posicion);
            int longitud = campo.longitudMaxima();
            int digitos = campo.tipoLongitud().digitosPrefijo();
            if (digitos > 0) {
                int posicionPrefijo = cursor.posicion;
                byte[] bytesLongitud = cursor.leer(digitos, numero);
                longitud = 0;
                for (byte valor : bytesLongitud) {
                    if (valor < '0' || valor > '9') {
                        throw error(CodigoErrorIso.LONGITUD_INVALIDA, numero, posicionPrefijo,
                                "El prefijo de longitud debe contener digitos ASCII");
                    }
                    longitud = longitud * 10 + valor - '0';
                }
                validarLongitud(campo, longitud, posicionPrefijo);
            }
            int posicionContenido = cursor.posicion;
            byte[] contenido = cursor.leer(longitud, numero);
            if (campo.tipoDato() == TipoDato.BINARIO) {
                constructor.campoBinario(numero, contenido);
            } else {
                validarTexto(contenido, campo, posicionContenido);
                constructor.campo(numero, new String(contenido, StandardCharsets.US_ASCII));
            }
        }
        if (cursor.posicion != datos.length) {
            throw error(CodigoErrorIso.DATOS_SOBRANTES, null, cursor.posicion,
                    "La trama contiene bytes despues del ultimo campo definido por el bitmap");
        }
        MensajeIso mensaje = constructor.construir();
        validarObligatorios(mensaje);
        return mensaje;
    }

    private DefinicionCampo definicion(int numero, int posicion) {
        DefinicionCampo campo = perfil.campos().get(numero);
        if (campo == null) {
            throw error(CodigoErrorIso.CAMPO_NO_DEFINIDO, numero, posicion,
                    "El campo " + numero + " no esta definido en el perfil");
        }
        return campo;
    }
    private byte[] codificarValor(ValorCampo valor, DefinicionCampo campo) {
        if (campo.tipoDato() == TipoDato.BINARIO && valor instanceof ValorBinario binario) {
            return binario.bytes();
        }
        if (campo.tipoDato() != TipoDato.BINARIO && valor instanceof ValorTexto texto) {
            if (texto.texto().chars().anyMatch(caracter -> caracter > 127)) {
                throw error(CodigoErrorIso.CODIFICACION_INVALIDA, campo.numero(), -1,
                        "El campo contiene caracteres no representables en ASCII");
            }
            byte[] contenido = texto.texto().getBytes(StandardCharsets.US_ASCII);
            validarTexto(contenido, campo, -1);
            return contenido;
        }
        throw error(CodigoErrorIso.CODIFICACION_INVALIDA, campo.numero(), -1,
                "El tipo del valor no coincide con la definicion del campo");
    }
    private void validarTexto(byte[] contenido, DefinicionCampo campo, int posicion) {
        for (int indice = 0; indice < contenido.length; indice++) {
            int valor = contenido[indice] & 0xFF;
            boolean valido = campo.tipoDato() == TipoDato.NUMERICO_ASCII
                    ? valor >= '0' && valor <= '9' : valor >= 32 && valor <= 126;
            if (!valido) {
                throw error(CodigoErrorIso.CODIFICACION_INVALIDA, campo.numero(), posicion < 0 ? -1 : posicion + indice,
                        "El campo contiene un byte no permitido por su tipo de dato");
            }
        }
    }
    private void validarLongitud(DefinicionCampo campo, int longitud, int posicion) {
        boolean valida = campo.tipoLongitud() == TipoLongitud.FIJA
                ? longitud == campo.longitudMaxima() : longitud <= campo.longitudMaxima();
        if (!valida) {
            throw error(CodigoErrorIso.LONGITUD_INVALIDA, campo.numero(), posicion,
                    "La longitud del campo " + campo.numero() + " no cumple su definicion");
        }
    }
    private void validarObligatorios(MensajeIso mensaje) {
        for (int numero : perfil.obligatorios(mensaje.mti())) {
            if (!mensaje.campos().containsKey(numero)) {
                throw error(CodigoErrorIso.CAMPO_OBLIGATORIO_AUSENTE, numero, -1,
                        "Falta el campo obligatorio " + numero + " para este MTI");
            }
        }
    }
    private void escribirBitmap(ByteArrayOutputStream salida, byte[] bitmap) {
        if (perfil.formatoBitmap() == FormatoBitmap.BINARIO) { salida.writeBytes(bitmap); return; }
        for (byte valor : bitmap) {
            salida.write(HEXADECIMALES[(valor & 0xFF) >>> 4]);
            salida.write(HEXADECIMALES[valor & 0x0F]);
        }
    }
    private byte[] leerBitmap(Cursor cursor) {
        if (perfil.formatoBitmap() == FormatoBitmap.BINARIO) { return cursor.leer(8, null); }
        int inicio = cursor.posicion;
        byte[] hexadecimal = cursor.leer(16, null);
        byte[] bitmap = new byte[8];
        for (int indice = 0; indice < hexadecimal.length; indice += 2) {
            bitmap[indice / 2] = (byte) ((nibble(hexadecimal[indice], inicio + indice) << 4)
                    | nibble(hexadecimal[indice + 1], inicio + indice + 1));
        }
        return bitmap;
    }
    private int nibble(byte valor, int posicion) {
        if (valor >= '0' && valor <= '9') { return valor - '0'; }
        if (valor >= 'A' && valor <= 'F') { return valor - 'A' + 10; }
        throw error(CodigoErrorIso.BITMAP_INVALIDO, null, posicion,
                "El bitmap debe contener hexadecimal ASCII en mayusculas");
    }
    private static void activarBit(byte[] bitmap, int numero) {
        bitmap[(numero - 1) / 8] |= (byte) (0x80 >>> ((numero - 1) % 8));
    }
    private static boolean bitActivo(byte[] bitmap, int numero) {
        return (bitmap[(numero - 1) / 8] & (0x80 >>> ((numero - 1) % 8))) != 0;
    }
    private static ExcepcionIso error(CodigoErrorIso codigo, Integer campo, int posicion, String texto) {
        return new ExcepcionIso(codigo, campo, posicion, texto);
    }
    private static final class Cursor {
        private final byte[] datos;
        private int posicion;
        private Cursor(byte[] datos) { this.datos = datos; }
        private byte[] leer(int longitud, Integer campo) {
            if (longitud < 0 || longitud > datos.length - posicion) {
                throw error(CodigoErrorIso.TRAMA_INCOMPLETA, campo, posicion,
                        "La trama no contiene los bytes requeridos en la posicion " + posicion);
            }
            byte[] contenido = Arrays.copyOfRange(datos, posicion, posicion + longitud);
            posicion += longitud;
            return contenido;
        }
    }
}
