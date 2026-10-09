# Investigación y alcance de ISO 8583

## Conclusión técnica

El problema se resuelve mediante una biblioteca de codificación, decodificación y validación por perfil. Un mensaje ISO 8583 no contiene por sí mismo todas las instrucciones necesarias para interpretar sus bytes: el integrador debe proporcionar la definición de campos y del sobre externo. Los motores existentes también usan especificaciones configurables, como muestra [GenericPackager de jPOS](https://jpos.org/docs/tutorial/iso-packager/generic-packager/).

El valor del proyecto estará en el contrato de perfiles, su validación, reglas entre mensajes, diagnósticos seguros y pruebas de interoperabilidad. Con la prioridad de evitar dependencias de ejecución, se recomienda implementar un núcleo Java propio y acotado. Los motores existentes servirán de comparación independiente; véase la [evaluación](09-evaluacion-motor-propio.md).

## Qué significa público

ISO 8583 es un estándar publicado, pero su texto completo no es automáticamente de libre reproducción. La ficha pública de [ISO 8583:2023](https://www.iso.org/standard/79451.html) identifica esa edición como vigente y remite las definiciones complementarias a una agencia de mantenimiento. El transporte y la liquidación quedan fuera de su alcance.

Los ejemplos públicos de bibliotecas ayudan a implementar codecs, pero no sustituyen la norma ni el contrato técnico de una contraparte. No se ofrecerá una tabla de campos como si fuera una reproducción autorizada de ISO.

## Ediciones y compatibilidad

| Objetivo | Tratamiento propuesto | Evidencia para publicarlo |
| --- | --- | --- |
| Variantes de estilo 1987 | Primer hito; ASCII y bitmap de 64/128 bits, después BCD y EBCDIC | Norma histórica autorizada cuando se reclame conformidad y vectores del perfil |
| 1993 y 2003 | Perfiles y reglas separados | Documentos de la edición elegida y pruebas propias |
| 2023 | Hito posterior; evaluar estructuras y datasets antes de reutilizar el modelo | Norma vigente y definiciones mantenidas aplicables |

La guía aportada declara una base de 1987. No debe reinterpretarse como 2023. Un MTI que señale una edición tampoco garantiza que la contraparte siga todos sus formatos.

## Definición de universalidad

Universal significa admitir formatos descritos por un perfil dentro de capacidades publicadas. No significa autodetectar cualquier dialecto, traducir semánticas de negocio automáticamente ni certificar interoperabilidad sin pruebas de contraparte.

El integrador seleccionará un perfil por endpoint o contrato. La heurística para sugerir perfiles podrá existir como herramienta de diagnóstico, pero no decidirá el parsing en producción. Un campo desconocido sin longitud conocida impide localizar el siguiente: el decoder debe detenerse, aunque tenga un modo tolerante.

## Alcance inicial

Se incluyen modelo de mensaje, compilación de perfiles, encode/decode, validación de presencia y formato, reglas de relación solicitud/respuesta y resultados con offsets. Se conservarán valores numéricos como cadenas para no perder ceros y datos binarios como bytes.

La autorización, contabilidad, reintentos financieros, reversos automáticos, sockets y administración de claves pertenecen a aplicaciones o adaptadores posteriores. La biblioteca podrá representar esos mensajes sin ejecutar sus efectos.

La decodificación correcta de un mensaje no demuestra que su importe, cuenta, autenticación o decisión de negocio sean válidos.
