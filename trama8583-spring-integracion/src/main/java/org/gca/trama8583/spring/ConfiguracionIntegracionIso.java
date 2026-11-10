package org.gca.trama8583.spring;

import org.gca.trama8583.nucleo.CodecIso;
import org.gca.trama8583.nucleo.CodecIsoBasico;
import org.gca.trama8583.nucleo.MensajeIso;
import org.gca.trama8583.nucleo.PerfilMensaje;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

/** Importar esta configuracion y proporcionar un bean PerfilMensaje del integrador. */
@Configuration(proxyBeanMethods = false)
@EnableIntegration
@IntegrationComponentScan(basePackageClasses = PasarelaIso.class)
public class ConfiguracionIntegracionIso {
    @Bean
    public CodecIso codecIso(PerfilMensaje perfil) { return new CodecIsoBasico(perfil); }

    @Bean(name = CanalesIso.DECODIFICACION)
    public DirectChannel canalDecodificacion() { return new DirectChannel(); }

    @Bean(name = CanalesIso.CODIFICACION)
    public DirectChannel canalCodificacion() { return new DirectChannel(); }

    @Bean
    public IntegrationFlow flujoDecodificacion(
            @Qualifier(CanalesIso.DECODIFICACION) MessageChannel entrada, CodecIso codec) {
        return IntegrationFlow.from(entrada)
                .transform(byte[].class, codec::decodificar)
                .get();
    }

    @Bean
    public IntegrationFlow flujoCodificacion(
            @Qualifier(CanalesIso.CODIFICACION) MessageChannel entrada, CodecIso codec) {
        return IntegrationFlow.from(entrada)
                .transform(MensajeIso.class, codec::codificar)
                .get();
    }
}
