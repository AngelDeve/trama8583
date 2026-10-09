# Plan de implementación y criterios de aceptación

## Hito 1 Corte vertical del motor propio

Crear el artefacto Java 21 y un corte del codec con MTI ASCII, bitmap ASCII hex de 64/128 bits, campos fijos, LLVAR ASCII y sobre configurable. Verificar los vectores existentes y añadir casos de fronteras y truncamiento. Comparar subconjuntos equivalentes con una implementación externa, sin incorporarla al runtime.

Entregable: evaluación del corte, matriz de funciones verificadas y árbol de dependencias que demuestre cero dependencias externas de compile/runtime. Esa evidencia permitirá estimar ampliaciones; no habrá promesa de conformidad o rendimiento basada sólo en el diseño.

## Hito 2 Perfiles y modelo

Completar API inmutable, builder Java, compilación de perfiles y validación de conflictos y reglas. Representar los dos ejemplos de laboratorio en el builder y resolver herencia. La configuración JSON quedará como módulo opcional posterior; no se requiere Jackson para usar el núcleo.

Aceptación: perfil reproducible por hash; fallo temprano ante configuraciones contradictorias, herencia circular o funciones no soportadas; `byte[]` sin alias mutable y sin valores sensibles en diagnósticos. Las dependencias de test quedan fuera del JAR y de sus dependencias transitivas de ejecución.

## Hito 3 Codec ASCII mínimo

Implementar MTI, bitmap 64/128, campos fijos y prefijos ASCII variables, sobre y validación estructural. Empezar por los vectores en `examples/messages/vectors.json`. Luego añadir BCD, binario y EBCDIC sólo con sus propios vectores.

Aceptación: bytes exactos de los casos canónicos, consumo completo de payload, errores deterministas sin lectura fuera de límites y ausencia de truncamiento silencioso. La aprobación de este hito demuestra el subconjunto de laboratorio, no un perfil completo de una red.

## Hito 4 Reglas y compuestos

Implementar reglas acotadas de presencia y relación solicitud/respuesta. Añadir mecanismos genéricos para subcampos y contenedores usando gramáticas sintéticas. El integrador aportará sus gramáticas privadas; los contenedores sin interpretación podrán permanecer opacos si están delimitados. Las reglas de eco recibirán contexto explícito; no habrá almacenamiento global de solicitudes.

Aceptación: matrices de casos permitidos/prohibidos, igualdad de campos configurados, preservación acotada de componentes opacos y rechazo de prefijos internos inconsistentes.

## Hito 5 Publicación

Incorporar README de instalación real, Javadoc, guía de migración de perfiles, changelog, licencia aprobada, avisos de terceros, SBOM y canal privado para reportar fallos. La integración continua se preparará con GitHub Actions para este repositorio y ejecutará compilación, vectores, pruebas de propiedades y análisis de dependencias.

La API propia, documentación, Javadoc, ayuda y diagnósticos cumplirán el [requisito de español para Latinoamérica](11-espanol-latinoamerica.md). Alinear las claves de configuración antes del primer esquema estable y comprobar que la localización no altere los bytes de las tramas.

Publicar el primer JAR sólo después de verificar que no contiene documentos, nombres ni datos restringidos. La homologación de una contraparte será un proceso adicional, con revisión del perfil exacto.

## Matriz de pruebas

| Familia | Casos necesarios | Resultado |
| --- | --- | --- |
| Bitmaps | Vacío, campos 2/64/65/128, secundario obligatorio, bloque truncado | Orden y fronteras correctas; bit de extensión coherente |
| Longitudes | 0, 1, máximo, máximo más 1, prefijo inválido o incompleto | Nunca leer/reservar más de los límites |
| Encodings | ASCII inválido, hex no válido, BCD impar y nibble no permitido, página EBCDIC | Falla o aceptación según perfil exacto |
| Sobre | Literal erróneo, cabecera corta, segmento inesperado | Offset preciso sin interpretación alternativa |
| Campos | Fijo corto/largo, desconocido activado, datos sobrantes | Rechazo explícito; opaco sólo si delimitado |
| Reglas | MTI/operación/resultado, requerido ausente, prohibido presente, eco cambiado | Diagnóstico con ID de regla |
| Compuestos | Conteo que incluye/excluye prefijo, token duplicado, anidación excesiva | Política explícita y límites |
| Framing futuro | Lecturas parciales, dos mensajes juntos, longitud desbordada | Independencia de límites TCP |
| Seguridad | Datos sensibles en errores/logs, path traversal, entidades externas | Ningún valor expuesto ni carga externa |
| Concurrencia | Mismo perfil en múltiples operaciones | Sin estado compartido corrupto |

Pruebas de propiedades: `decode(encode(m))` conserva valores lógicos para mensajes válidos. `encode(decode(bytes))` conserva bytes sólo en formatos canónicos declarados. Mutar bitmap y prefijos y truncar cada offset debe devolver error controlado. Comparar con otro motor requiere perfiles idénticos, no simplemente el mismo nombre de edición.

Las métricas de rendimiento se medirán después: mensajes por segundo, percentiles de latencia, bytes reservados por operación y tamaños de payload, indicando JVM, hardware y perfil. No se promete throughput sin medición.

## Decisiones antes de programar

Java queda seleccionado y Apache 2.0 es la licencia objetivo. El mantenedor aceptó el motor propio sin dependencias de ejecución el 9 de octubre de 2026. Falta comprobar el corte vertical, formalizar la licencia y preparar la publicación de Trama8583. La falta de manuales recientes, definiciones de mantenimiento, gramáticas privadas o HSM no bloquea el núcleo configurable. Esos recursos sólo se pedirán para una integración o capacidad específica que los necesite.
