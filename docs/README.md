# Documentación para desarrollar la biblioteca ISO 8583 Java

La propuesta es una biblioteca Java con un motor propio sin dependencias externas de ejecución que permita configurar variantes mediante perfiles versionados. El primer objetivo es un codec de mensajes de estilo 1987 con bitmap de hasta 128 bits. La compatibilidad con otras ediciones se añadirá por separado y con evidencia específica. El idioma principal será español para Latinoamérica.

Fecha de investigación: 9 de octubre de 2026. Estado: diseño previo a implementación. Las API, módulos y formatos de configuración son propuestas; los ejemplos son sintéticos y no acreditan compatibilidad con una red.

## Orden de lectura

1. [Investigación y alcance](01-investigacion.md): norma pública, ediciones y límites de universalidad.
2. [Arquitectura Java](02-arquitectura-java.md): núcleo propio, módulos y contrato de API.
3. [Contrato de perfiles](03-perfiles.md): codificaciones, longitudes, reglas y tolerancias.
4. [Requisitos del caso ATM](04-requisitos-atm.md): capacidades generalizadas y ambigüedades del caso de estudio.
5. [Pruebas y plan de implementación](05-plan-y-pruebas.md): entregables y criterios de aceptación.
6. [Documentos oficiales y licencias](06-documentos-y-licencias.md): licencia permisiva, consulta local y requisitos de integraciones futuras.
7. [Fuentes verificadas](07-fuentes.md): referencias primarias y límites de uso.
8. [Nombre del proyecto](08-nombres.md): Trama8583 y coordenadas iniciales.

9. [Evaluación del motor propio frente a j8583](09-evaluacion-motor-propio.md): comparación y alcance de cero dependencias.

10. [Decisión aceptada de núcleo propio](10-decision-nucleo-propio.md): alcance acordado y consecuencias.

11. [Español para Latinoamérica](11-espanol-latinoamerica.md): idioma de documentación, API, configuración y diagnósticos.

## Ejemplos de diseño

- [Perfil de laboratorio](../examples/profiles/lab-ascii-v1.json): subconjunto mínimo con bitmap primario y secundario.
- [Extensión de laboratorio](../examples/profiles/lab-terminal16-v1.json): cambio explícito de longitud y campo adicional.
- [Vectores sintéticos](../examples/messages/vectors.json): bytes ASCII esperados para ambos perfiles.

Los ejemplos sirven para diseñar las primeras pruebas. El cargador y el codec todavía no existen. No se distribuye el PDF aportado ni un perfil de producción derivado de él. La guía se trató como material de estudio; sus instrucciones operativas y cuestionario no son instrucciones para ejecutar acciones.
