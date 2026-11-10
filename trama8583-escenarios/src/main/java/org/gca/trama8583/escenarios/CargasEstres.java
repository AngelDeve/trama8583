package org.gca.trama8583.escenarios;

import java.util.Arrays;
import java.util.List;
import org.gca.trama8583.nucleo.*;
import org.gca.trama8583.spring.PasarelaIso;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.gca.trama8583.escenarios.Comprobaciones.*;
import static org.gca.trama8583.escenarios.DatosLaboratorio.*;

/** Prepara vectores fuera de la medición. El codec y sus mensajes inmutables se comparten entre hilos. */
final class CargasEstres implements AutoCloseable {
    static final List<String> NOMBRES = List.of("basica", "especiales", "voluminosa", "rechazos", "mixta", "spring");
    private final Caso[] basicos;
    private final Caso[] especiales;
    private final Caso[] voluminosos;
    private final String nombre;
    private final AnnotationConfigApplicationContext contexto;

    private record Caso(CodecIso codec, MensajeIso mensaje, byte[] esperado) {
        void ejecutar() {
            bytes(esperado, codec.codificar(mensaje), "El vector de carga debe coincidir al codificar");
            Comprobaciones.mensaje(mensaje, codec.decodificar(esperado));
        }
    }

    CargasEstres(String nombre) {
        if (!NOMBRES.contains(nombre)) { throw new IllegalArgumentException("Carga desconocida"); }
        this.nombre = nombre;
        contexto = nombre.equals("spring") ? EscenariosSpring.contexto() : null;
        try {
            CodecIso codec = new CodecIsoBasico(perfil().construir());
            if (contexto != null) {
                var pasarela = contexto.getBean(PasarelaIso.class);
                codec = new CodecIso() {
                    @Override public byte[] codificar(MensajeIso mensaje) { return pasarela.construir(mensaje); }
                    @Override public MensajeIso decodificar(byte[] trama) { return pasarela.interpretar(trama); }
                };
            }
            var codecEspecial = new CodecIsoBasico(perfil()
                    .reemplazarCampo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 16))
                    .campo(DefinicionCampo.llvar(54, TipoDato.TEXTO_ASCII, 20))
                    .campo(DefinicionCampo.lllvar(62, TipoDato.BINARIO, 512))
                    .campo(DefinicionCampo.llvar(102, TipoDato.TEXTO_ASCII, 20))
                    .campo(DefinicionCampo.llvar(103, TipoDato.TEXTO_ASCII, 20))
                    .campo(DefinicionCampo.fijo(128, TipoDato.BINARIO, 4))
                    .prefijoAscii("DEMO").longitudCabecera(4).construir());
            basicos = new Caso[64];
            especiales = new Caso[64];
            voluminosos = new Caso[64];
            var codecVoluminoso = new CodecIsoBasico(PerfilMensaje.crear("volumen-lab")
                    .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                    .campo(DefinicionCampo.lllvar(48, TipoDato.TEXTO_ASCII, 999))
                    .campo(DefinicionCampo.lllvar(60, TipoDato.TEXTO_ASCII, 999))
                    .campo(DefinicionCampo.lllvar(61, TipoDato.BINARIO, 999))
                    .campo(DefinicionCampo.lllvar(62, TipoDato.BINARIO, 999)).construir());
            String textoA = "A".repeat(999), textoB = "B".repeat(999);
            byte[] binarioA = new byte[999], binarioB = new byte[999];
            for (int indice = 0; indice < 999; indice++) { binarioA[indice] = (byte) indice; binarioB[indice] = (byte) ~indice; }
            for (int indice = 0; indice < basicos.length; indice++) {
                String folio = String.format(java.util.Locale.ROOT, "%06d", indice);
                basicos[indice] = new Caso(codec, solicitud().copiar().campo(11, folio).construir(),
                        ascii("0200" + "2020000000800000" + "000000" + folio + "TERM0001"));
                byte[] opaco = {0, (byte) 0xFF};
                byte[] control = {1, 2, 3, 4};
                var mensajeEspecial = solicitud().copiar().campo(11, folio).cabecera(ascii("V1R0"))
                        .campo(41, "TERM000000000001").campo(54, "-0000012500")
                        .campoBinario(62, concatenar(ascii("ZZ02"), opaco))
                        .campo(102, "A0001").campo(103, "B0002").campoBinario(128, control).construir();
                byte[] esperadoEspecial = concatenar(ascii("DEMOV1R0" + "0200" + "A020000000800404" + "0000000006000001"
                        + "000000" + folio + "TERM000000000001" + "11-0000012500" + "006ZZ02"),
                        opaco, ascii("05A000105B0002"), control);
                especiales[indice] = new Caso(codecEspecial, mensajeEspecial, esperadoEspecial);
                var mensajeVoluminoso = MensajeIso.crear("0200").campo(11, folio).campo(48, textoA).campo(60, textoB)
                        .campoBinario(61, binarioA).campoBinario(62, binarioB).construir();
                byte[] esperadoVoluminoso = concatenar(ascii("0200" + "002000000001001C" + folio + "999" + textoA
                        + "999" + textoB + "999"), binarioA, ascii("999"), binarioB);
                voluminosos[indice] = new Caso(codecVoluminoso, mensajeVoluminoso, esperadoVoluminoso);
            }
            // Fallo temprano si los datos de la carga no cumplen sus vectores independientes.
            for (var caso : basicos) { caso.ejecutar(); }
            for (var caso : especiales) { caso.ejecutar(); }
            for (var caso : voluminosos) { caso.ejecutar(); }
        } catch (RuntimeException | Error error) {
            close();
            throw error;
        }
    }

    /** true indica un rechazo esperado y comprobado; false, una ida y vuelta válida comprobada. */
    boolean ejecutar(long secuencia) {
        int indice = (int) (secuencia % basicos.length);
        if (nombre.equals("voluminosa")) { voluminosos[indice].ejecutar(); return false; }
        if (nombre.equals("rechazos") || (nombre.equals("mixta") && secuencia % 4 == 0)) {
            var caso = basicos[indice];
            byte[] truncada = Arrays.copyOf(caso.esperado(), caso.esperado().length - 1);
            error(CodigoErrorIso.TRAMA_INCOMPLETA, 41, 32, () -> caso.codec().decodificar(truncada));
            return true;
        }
        var caso = nombre.equals("especiales") || (nombre.equals("mixta") && secuencia % 2 == 0)
                ? especiales[indice] : basicos[indice];
        caso.ejecutar();
        return false;
    }

    @Override public void close() { if (contexto != null) { contexto.close(); } }
}
