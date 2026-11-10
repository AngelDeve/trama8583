package org.gca.trama8583.escenarios;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/** Vectores de extremo a extremo contra un servidor de laboratorio con longitud binaria BE de 2 bytes. */
public final class EscenariosTcp {
    private static final HexFormat HEX = HexFormat.of().withUpperCase();
    private static final int MAXIMO_BYTES = 4096;

    public enum Caso {
        FINANCIERO("0200 → 0210", DatosLaboratorio.tramaSolicitud(), DatosLaboratorio.tramaRespuesta()),
        RED("0800 → 0810",
                Comprobaciones.ascii("0800" + "80200000000000000400000000000000" + "000002301"),
                Comprobaciones.ascii("0810" + "80200000020000000400000000000000" + "00000200301"));

        private final String intercambio;
        private final byte[] solicitud;
        private final byte[] respuesta;

        Caso(String intercambio, byte[] solicitud, byte[] respuesta) {
            this.intercambio = intercambio;
            this.solicitud = enmarcar(solicitud);
            this.respuesta = enmarcar(respuesta);
        }
    }

    public record Resultado(String intercambio, boolean correcto, String diagnostico,
                            String solicitudHex, String respuestaHex, long milisegundos) { }

    private EscenariosTcp() { }

    public static Resultado probar(Caso caso, String host, int puerto, int esperaMs) {
        long inicio = System.nanoTime();
        var recibido = new ByteArrayOutputStream();
        String diagnostico;
        boolean correcto = false;
        try (var socket = new Socket(Proxy.NO_PROXY)) {
            var direccion = new InetSocketAddress(host, puerto);
            if (direccion.isUnresolved()) { throw new ConnectException("host sin resolver"); }
            long limite = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(esperaMs);
            socket.connect(direccion, esperaMs);
            socket.getOutputStream().write(caso.solicitud);
            var entrada = socket.getInputStream();
            leer(entrada, socket, recibido, 2, limite);
            byte[] prefijo = recibido.toByteArray();
            int bytes = ((prefijo[0] & 255) << 8) | (prefijo[1] & 255);
            if (bytes < 1 || bytes > MAXIMO_BYTES) {
                throw new IllegalArgumentException("El servidor anunció una longitud fuera del rango 1–4096.");
            }
            leer(entrada, socket, recibido, bytes, limite);
            correcto = Arrays.equals(caso.respuesta, recibido.toByteArray());
            diagnostico = correcto ? "La respuesta coincide byte a byte con el vector esperado. Campo 39 = 00."
                    : "La respuesta no coincide con el vector esperado.";
        } catch (SocketTimeoutException error) {
            diagnostico = "Se agotó el tiempo de conexión o respuesta.";
        } catch (ConnectException error) {
            diagnostico = "No se pudo conectar al host y puerto indicados.";
        } catch (EOFException error) {
            diagnostico = "El servidor cerró antes de completar la respuesta.";
        } catch (IllegalArgumentException error) {
            diagnostico = error.getMessage();
        } catch (IOException error) {
            diagnostico = "Falló el intercambio TCP. Tipo " + error.getClass().getSimpleName() + ".";
        }
        return new Resultado(caso.intercambio, correcto, diagnostico, HEX.formatHex(caso.solicitud),
                HEX.formatHex(recibido.toByteArray()), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
    }

    private static byte[] enmarcar(byte[] datos) {
        return ByteBuffer.allocate(2 + datos.length).putShort((short) datos.length).put(datos).array();
    }

    private static void leer(InputStream entrada, Socket socket, ByteArrayOutputStream recibido,
                             int cantidad, long limite) throws IOException {
        byte[] bloque = new byte[cantidad];
        int faltantes = cantidad;
        while (faltantes > 0) {
            long disponible = limite - System.nanoTime();
            if (disponible <= 0) { throw new SocketTimeoutException(); }
            socket.setSoTimeout((int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(disponible)));
            int leidos = entrada.read(bloque, 0, faltantes);
            if (leidos == -1) { throw new EOFException(); }
            recibido.write(bloque, 0, leidos);
            faltantes -= leidos;
        }
    }
}
