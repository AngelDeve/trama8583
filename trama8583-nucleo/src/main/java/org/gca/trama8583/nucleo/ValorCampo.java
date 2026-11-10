package org.gca.trama8583.nucleo;

/** Valor inmutable cuyo toString nunca expone datos del mensaje. */
public sealed interface ValorCampo permits ValorTexto, ValorBinario { }
