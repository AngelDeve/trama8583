# Decisión de implementar un núcleo propio Java

Estado: aceptada por el mantenedor el 9 de octubre de 2026.

## Decisión

La biblioteca tendrá un núcleo propio en Java, sin dependencias externas de compilación o ejecución en el artefacto principal. Utilizará las APIs del JDK y perfiles explícitos para describir las particularidades de cada formato. j8583 y jPOS quedan como referencias o comparadores externos de pruebas.

## Consecuencias

El proyecto controla su modelo, encodings, longitudes, límites y diagnósticos. También asume la implementación y mantenimiento del parser y la verificación de sus formatos. Se requieren vectores independientes y pruebas negativas además de pruebas de ida y vuelta.

La configuración inicial será mediante builder Java. Los cargadores JSON y otras integraciones serán módulos opcionales, sin dependencias transitivas desde el núcleo. Las herramientas de pruebas y desarrollo podrán utilizar bibliotecas externas sin incorporarlas al runtime entregado.

## Primer entregable previsto

Un codec acotado con MTI ASCII, bitmap hexadecimal ASCII de 64/128 bits, campos fijos y LLVAR ASCII, sobre configurable y errores con offsets sin datos sensibles. Se validará con vectores sintéticos antes de ampliar encodings y compuestos.

El primer núcleo ya está implementado con Java 21, bitmap ASCII o binario, FIJA/LLVAR/LLLVAR y los tres vectores de laboratorio. Por solicitud del mantenedor, un módulo separado integra canales y una pasarela con Spring Integration; el núcleo mantiene cero dependencias externas de ejecución. La [guía Spring](12-integracion-spring.md) delimita las capacidades actuales. Apache 2.0 sigue como licencia objetivo pendiente de formalizar. Las normas y manuales de consulta permanecen fuera del repositorio.

La [evaluación comparativa](09-evaluacion-motor-propio.md) conserva las razones y alternativas consideradas; el [plan de implementación](05-plan-y-pruebas.md) establece los criterios de aceptación.
