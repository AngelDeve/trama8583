package org.gca.trama8583.nucleo;

/** Operaciones sin efectos de negocio ni dependencias de transporte. */
public interface CodecIso {
    byte[] codificar(MensajeIso mensaje);
    MensajeIso decodificar(byte[] datos);
}
