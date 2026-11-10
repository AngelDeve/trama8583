# Documentación para desarrollar la biblioteca ISO 8583 Java

La propuesta es una biblioteca Java con un motor propio sin dependencias externas de ejecución que permita configurar variantes mediante perfiles versionados. El primer objetivo es un codec de mensajes de estilo 1987 con bitmap de hasta 128 bits. La compatibilidad con otras ediciones se añadirá por separado y con evidencia específica. El idioma principal será español para Latinoamérica.

Fecha de investigación: 9 de octubre de 2026. Estado: primera implementación de laboratorio. Los documentos 01–11 conservan propuestas y decisiones de diseño; el documento 12 y el README raíz delimitan las capacidades implementadas. Los ejemplos son sintéticos y no acreditan compatibilidad con una red.

## Orden de lectura

1. [Investigación y alcance](01-investigacion.md): norma pública, ediciones y límites de universalidad.
2. [Arquitectura Java](02-arquitectura-java.md): núcleo propio, módulos y contrato de API.
3. [Contrato de perfiles](03-perfiles.md): codificaciones, longitudes, reglas y tolerancias.
4. [Variaciones de mensajería](04-requisitos-atm.md): formatos alternativos y escenarios sintéticos de laboratorio.
5. [Pruebas y plan de implementación](05-plan-y-pruebas.md): entregables y criterios de aceptación.
6. [Documentos oficiales y licencias](06-documentos-y-licencias.md): licencia permisiva, consulta local y requisitos de integraciones futuras.
7. [Fuentes verificadas](07-fuentes.md): referencias primarias y límites de uso.
8. [Nombre del proyecto](08-nombres.md): Trama8583 y coordenadas iniciales.

9. [Evaluación del motor propio frente a j8583](09-evaluacion-motor-propio.md): comparación y alcance de cero dependencias.

10. [Decisión aceptada de núcleo propio](10-decision-nucleo-propio.md): alcance acordado y consecuencias.

11. [Español para Latinoamérica](11-espanol-latinoamerica.md): idioma de documentación, API, configuración y diagnósticos.

12. [Integración Spring y primer núcleo](12-integracion-spring.md): módulos reales, pasarela, canales, errores y ejecución.

13. [Matriz de escenarios JUnit](13-matriz-pruebas-junit.md): casos válidos, errores, límites, inmutabilidad, Spring y ejecución por módulo.

14. [Catálogo de escenarios ejecutables](../trama8583-escenarios/README.md): recetas Dado / Cuando / Entonces, ejecución autónoma por ID y guía para agregar casos de mantenimiento.

15. [Pruebas de estrés](15-pruebas-estres.md): cargas concurrentes, métricas, reportes JSON/CSV, umbrales y automatizaciones.

## Ejemplos de diseño

- [Perfil de laboratorio](../examples/profiles/lab-ascii-v1.json): subconjunto mínimo con bitmap primario y secundario.
- [Extensión de laboratorio](../examples/profiles/lab-terminal16-v1.json): cambio explícito de longitud y campo adicional.
- [Vectores sintéticos](../examples/messages/vectors.json): bytes ASCII esperados para ambos perfiles.

Los tres vectores se utilizan como resultados esperados en las pruebas del codec. El cargador JSON todavía no existe; los perfiles ejecutables se construyen mediante la API Java. Los perfiles públicos son ejemplos de laboratorio y no describen una integración de producción.
