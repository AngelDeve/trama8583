# Matriz de escenarios JUnit

La suite usa JUnit Jupiter 6.0.3 y pruebas parametrizadas y dinámicas con nombres en español. Verificación del 15 de octubre de 2026: **406 casos**, sin fallos, errores ni pruebas omitidas. Son 337 pruebas del núcleo, 20 de integración Spring, 1 de la aplicación de ejemplo y 48 del módulo de escenarios. Estas últimas incluyen 30 recetas ejecutables, una comprobación de integridad del catálogo, cuatro pruebas del ejecutor y 13 comprobaciones de cargas, mediciones e histograma. Cada combinación parametrizada o escenario dinámico cuenta como un caso; las pruebas de concurrencia y entradas aleatorias realizan además varias operaciones dentro de su caso.

## Escenarios del núcleo

| Familia | Escenarios verificados | Suites |
| --- | --- | --- |
| Vectores independientes | Primario, secundario y variante de terminal de 16 bytes, con bytes exactos esperados | `CodecIsoBasicoTest` |
| Formatos y longitudes | 96 combinaciones de dos formatos de bitmap, secundario opcional/obligatorio, tres tipos de dato y ocho casos de longitud; prefijos con ceros, contenido vacío y máximos 99/999 | `CodecEscenariosTest` |
| Bitmap y orden | Campos 2/64/65/128, todos los campos 2–128 insertados en orden inverso, hexadecimal en mayúsculas, secundario vacío y rechazo de primario cuando se exige secundario | Ambas suites de codec |
| Truncamientos | Cada corte del vector primario y 92 cortes del sobre mixto en formato hexadecimal/binario: literal, cabecera, MTI, ambos bitmaps, prefijos y contenido de LLVAR/LLLVAR y campo fijo | Ambas suites de codec |
| Longitudes inválidas | Fijo vacío/corto/largo, variable por encima del máximo, prefijo no decimal o no ASCII, contenido anunciado pero incompleto, longitud extrema sin reservar memoria | `CodecEscenariosTest`, `CodecIsoBasicoTest` |
| Codificación | Tipos lógicos incompatibles, numérico con signos/letras/espacios, controles, DEL y caracteres Unicode; fronteras ASCII al leer; todos los bytes 0–255 en binario y todos los caracteres ASCII imprimibles | `CodecEscenariosTest` |
| Sobre y límites | Prefijos ASCII y opacos, literal incorrecto, cabecera de longitud incorrecta, límite total exacto y un byte de más, sobre secundario que no cabe | Ambas suites de codec |
| Diagnósticos | Códigos, campo y posición absoluta desde cero, incluido sobre; `-1` al codificar; obligatorio ausente, campo desconocido y datos sobrantes; ausencia de valores en diagnósticos | Ambas suites de codec |
| Robustez | 1000 entradas aleatorias reproducibles sin errores de índice, límites antes de leer/reservar, argumentos nulos | Ambas suites de codec |
| Perfiles | Identificador ausente, longitud no representable, números fuera de rango, mínimo del sobre, cabeceras negativas/extremas, duplicados y reemplazo explícito, obligatorios por MTI, obligatoriedad sin definición y parámetros nulos | `PerfilMensajeTest`, `ModeloIsoTest` |
| Mensajes y valores | MTI inválido, campos fuera de rango, crear/copiar/reemplazar/quitar, orden, acceso a texto ausente o binario, ceros/espacios conservados, igualdad de valores binarios y rechazo de nulos | `MensajeIsoTest`, `ModeloIsoTest` |
| Inmutabilidad | Reutilización de constructores sin cambiar instantáneas, mapas/conjuntos no modificables, copias defensivas de prefijo/cabecera/campos, entrada/salida del codec sin alias, `toString()` sin valores | Suites de modelo, perfiles y codec |

Los bytes esperados se construyen a partir de literales, segmentos y contenido sintético definido en las pruebas; no se obtienen llamando al codec bajo prueba. Las comprobaciones de ida y vuelta complementan esos resultados independientes.

## Integración y ejemplo

| Familia | Escenarios verificados | Suite |
| --- | --- | --- |
| Pasarela | Construir/interpretar solicitud, respuesta y secundario; MTI conservado | `IntegracionIsoTest`, `CanalesEscenariosTest` |
| Errores de pasarela | Truncamiento, MTI, bitmap, desconocido, obligatorio, longitud, codificación y sobrantes conservan código/campo/posición en la causa; validación al construir y procesamiento después de un error | `CanalesEscenariosTest` |
| Canales reales | Cabeceras de aplicación conservadas, mensaje original intacto, payload incompatible rechazado, codec ejecutado en el hilo del llamador | Ambas suites Spring |
| Configuración | Fallo cuando falta el perfil o hay dos perfiles sin selección explícita | `CanalesEscenariosTest` |
| Concurrencia | 100 llamadas desde cuatro hilos sin cruzar folios; tiempo máximo de la ejecución concurrente acotado para detectar bloqueos | `IntegracionIsoTest` |
| Spring Boot | Arranque real, ejecución del ejemplo, respuesta sintética de 42 bytes procesada y contexto cerrado | `AplicacionTrama8583Test` |

Las pruebas del núcleo son unitarias y no abren Spring. Las de canales usan un contexto Spring real para comprobar el contrato del framework; la del ejemplo es una prueba de arranque Boot. No necesitan red, HSM, base de datos ni documentación privada. JUnit es una dependencia con alcance `test` en cada módulo.

## Escenarios de uso autónomos

El módulo `trama8583-escenarios` complementa las pruebas técnicas con [30 recetas documentadas](../trama8583-escenarios/README.md), ejecutables individualmente y sin servicios externos. El catálogo registra el contrato Dado / Cuando / Entonces y una única acción de comprobación que se utiliza tanto desde el JAR como desde JUnit. Incluye nueve escenarios del núcleo, ocho rechazos esperados, diez casos especiales de laboratorio y tres de Spring. Las reglas entre mensajes y el contenedor sintético se prueban como ejemplos de aplicación, sin atribuir esas capacidades al núcleo. Los vectores esperados son independientes del codec y los errores se comprueban por código, campo y posición.

GitHub Actions ejecuta `verify`, el catálogo desde el JAR empaquetado y una carga concurrente breve. El workflow manual de [estrés](15-pruebas-estres.md) genera mediciones repetidas y reportes JSON/CSV. Un fallo de contrato hace fallar la validación automática; un error esperado correctamente diagnosticado cuenta como escenario correcto.

## Ejecutar y consultar resultados


Con `JAVA_HOME` apuntando al JDK 21, desde el repositorio:

```sh
# Suite completa, tambien utilizada por GitHub Actions
./mvnw --batch-mode --no-transfer-progress verify

# Solo el nucleo
./mvnw -pl trama8583-nucleo test

# Nucleo y adaptador Spring con sus dependencias del reactor
./mvnw -pl trama8583-spring-integracion -am test

# Catalogo de uso y sus dependencias
./mvnw -pl trama8583-escenarios -am verify

# Mismos escenarios desde la distribucion autonoma
"$JAVA_HOME/bin/java" -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar --todos
```

En IntelliJ puedes ejecutar una clase o escenario desde `src/test/java`, con JDK 21. Maven deja reportes de texto y XML en `target/surefire-reports/` de cada módulo. El conteo corresponde a la suite actual, no a un porcentaje de cobertura de líneas.

## Alcance pendiente

La matriz verifica el contrato implementado. BCD, EBCDIC, cargadores JSON/YAML, tercer bitmap, reglas compuestas/TLV, TCP y PIN/MAC/HSM no existen todavía y necesitarán sus propios escenarios al incorporarse. Los casos binarios de campo 65 se limitan a datos explícitos del perfil de laboratorio; no prueban un tercer bitmap. Los vectores sintéticos no certifican compatibilidad con una red ni sustituyen una comparación externa con un perfil equivalente.
