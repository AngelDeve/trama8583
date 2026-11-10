package org.gca.trama8583.spring;

import org.gca.trama8583.nucleo.MensajeIso;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

/** Fachada de alto nivel; cada llamada recorre un flujo de Spring Integration. */
@MessagingGateway(name = "pasarelaIso", defaultRequestTimeout = "5000", defaultReplyTimeout = "5000")
public interface PasarelaIso {
    @Gateway(requestChannel = CanalesIso.DECODIFICACION)
    MensajeIso interpretar(byte[] trama);

    @Gateway(requestChannel = CanalesIso.CODIFICACION)
    byte[] construir(MensajeIso mensaje);
}
