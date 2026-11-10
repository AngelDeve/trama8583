# Variaciones de mensajería y escenarios de laboratorio

Los ejemplos de este proyecto usan perfiles, valores y gramáticas sintéticos para mostrar posibilidades de configuración. No representan el contrato de una red. El perfil de cada integración debe definir de forma explícita sus formatos y reglas.

## Capacidades y ejemplos

| ID | Capacidad | Ejemplo ejecutable | Capa que lo resuelve |
| --- | --- | --- | --- |
| ATM-01 | Prefijo constante antes de cabecera y MTI | NUC-06, ESP-01 | Codec y perfil |
| ATM-02 | Cabecera opaca con versión y estado independientes del cuerpo | ESP-01 | Transporte por el codec; interpretación por la aplicación |
| ATM-03 | Bitmap hexadecimal o binario y secundario opcional u obligatorio | NUC-04, NUC-05, NUC-08 | Codec y perfil |
| ATM-04 | Terminal de distinta longitud según perfil | NUC-07 | Reemplazo explícito de definición |
| ATM-05 | Monto fijo o variable; signo como texto configurado | ESP-02 | Codec para representación; aplicación para semántica monetaria |
| ATM-06 | Presencia condicionada por operación y resultado | ESP-05 | Regla de ejemplo en la aplicación |
| ATM-07 | Referencias ficticias de origen y destino con prefijos separados | ESP-03 | Campos LLVAR del perfil |
| ATM-08 | Eco de folio y terminal entre solicitud y respuesta | ESP-04 | Regla de ejemplo con contexto explícito |
| ATM-09 | Contenedor con prefijo externo y longitudes internas diferentes | ESP-06, ESP-08 | Campo del codec y parser de ejemplo en la aplicación |
| ATM-10 | Tokens desconocidos conservados como bytes opacos | ESP-07 | Campo binario y contenedor de ejemplo |
| ATM-11 | Estado del sobre separado del resultado del cuerpo | ESP-01 | Interpretación de la aplicación |
| ATM-12 | Mensajes financieros, de reverso y de red | NUC-01, NUC-02, ESP-09, ESP-10 | Modelo y codec; ciclos de vida fuera del núcleo |

Consulta el [catálogo ejecutable](../trama8583-escenarios/README.md). La familia ESP registra estas variaciones como Dado / Cuando / Entonces. Las reglas y el contenedor viven únicamente en el módulo de ejemplos; el núcleo todavía no ofrece un motor de reglas entre campos ni un parser genérico de compuestos.

## Decisiones que cada perfil debe fijar

- Orden y longitud de cada segmento del sobre; significado de su versión y estado.
- Tipo de dato y codificación de cada campo, máximo y unidad de longitud.
- Si un prefijo interno cuenta el valor, la cabecera interna o el contenedor completo.
- Presencia por MTI y condiciones de operación o resultado; campos que deben conservarse entre mensajes.
- Política explícita de signo, ceros y espacios. El codec no completa datos ni deduce formatos.
- Gramática de contenedores, límites, duplicados y conservación de elementos opacos.

Un dato que no cumpla el perfil debe rechazarse con un diagnóstico controlado. Las variantes se configuran de forma explícita; no se prueban formatos alternativos hasta encontrar uno que parezca válido.

## Límites de los ejemplos

Los identificadores de cuenta son ficticios. Los montos se conservan como cadenas; moneda, exponente y reglas de negocio pertenecen a la aplicación. Los campos binarios no realizan operaciones criptográficas.

La gramática AA/BB/ZZ es inventada: etiqueta ASCII de dos bytes, longitud decimal ASCII de dos bytes y valor opaco. No implementa un estándar de tokens ni un protocolo de red. Sirve para comprobar delimitación, conservación de bytes y rechazo de longitudes inconsistentes.

El ejemplo de reversos demuestra representación de mensajes y relleno explícito. Temporizadores, reintentos, duplicados y correlación son responsabilidades de la aplicación.

## Medición

La [guía de estrés](15-pruebas-estres.md) automatiza cargas básicas, especiales, voluminosas, negativas, mixtas y Spring. Las mediciones incluyen comprobación de resultados, entorno de ejecución y reportes comparables por carga y número de hilos.
