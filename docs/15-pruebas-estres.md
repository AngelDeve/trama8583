# Pruebas de estrés y mediciones reproducibles

El JAR de `trama8583-escenarios` incluye un ejecutor de carga concurrente. Comprueba resultados durante la medición, comparte codecs inmutables entre hilos y produce reportes JSON y CSV. El núcleo mantiene sus dependencias de ejecución limitadas al JDK.

## Ejecutar una medición

Desde la raíz del repositorio, con Java 21:

```sh
./mvnw --batch-mode --no-transfer-progress -pl trama8583-escenarios -am verify

"$JAVA_HOME/bin/java" -Xms256m -Xmx256m \
  -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar \
  --estres --carga especiales --hilos 4 --calentamiento 5 --segundos 30 \
  --salida target/estres/especiales-4h-01.json
```

Se generan `especiales-4h-01.json` y `especiales-4h-01.csv`. Usa nombres distintos para conservar cada repetición; el mismo nombre sobrescribe la medición anterior. El JAR ya compilado funciona fuera del repositorio sin Maven ni servicios externos. Usa `java` en Windows y `mvnw.cmd` para compilar.

`--estres --listar` muestra las cargas y `--estres --ayuda` describe los argumentos. Para una comprobación determinista de conteos, `--operaciones-por-hilo 1000` reemplaza la duración medida por 1000 operaciones por trabajador; el calentamiento sigue siendo temporal. El valor `0` usa el modo por duración.

## Cargas disponibles

| Carga | Trabajo por operación |
| --- | --- |
| `basica` | Construir una solicitud de 40 bytes, comparar con su vector independiente y leer ese vector comprobando MTI, campos y cabecera. |
| `especiales` | Codificar y leer un mensaje con sobre versionado, terminal de 16 bytes, monto de texto con signo, campos 102/103, contenedor binario y campo 128 opaco. Incluye comparación con bytes esperados. |
| `voluminosa` | Codificar y leer 4034 bytes: folio y cuatro campos LLLVAR de 999 bytes, con texto y bytes binarios. Comprueba los bytes y todos los valores. |
| `rechazos` | Interpretar una trama truncada y exigir código, campo y posición exactos. Cada rechazo correcto se cuenta como esperado. |
| `mixta` | Cada trabajador alterna cargas: dos básicas, una especial y un rechazo por ciclo de cuatro operaciones. |
| `spring` | Ejecutar la carga básica a través de una pasarela y flujos Spring compartidos. El contexto se crea antes de medir y se cierra al terminar. |

Hay 64 folios sintéticos por carga para comprobar respuestas distintas al compartir el codec. Los vectores se preparan y validan antes del calentamiento. La medición incluye asignaciones y verificaciones del camino ejecutado, pero excluye construcción de perfiles y arranque de Spring.

Una operación válida incluye **codificación y decodificación** con comprobación. Una operación negativa incluye una interpretación rechazada y la comprobación del diagnóstico. `operacionesPorSegundo` no representa transacciones financieras ni mensajes individuales por segundo. Compara cada carga con la misma carga.

## Métricas

| Datos del reporte | Interpretación |
| --- | --- |
| Operaciones, idas y vueltas correctas, rechazos esperados y errores inesperados | Los tres contadores de resultado suman las operaciones medidas. Cualquier error inesperado hace fallar la ejecución. |
| Duración real y operaciones por segundo | Tiempo de pared desde la liberación simultánea de los trabajadores hasta completar y reunir sus resultados. |
| Mínima, media y máxima en nanosegundos | Duración instrumentada de cada operación, incluidas sus comprobaciones. |
| `p50NsLimiteSuperior`, `p95NsLimiteSuperior`, `p99NsLimiteSuperior` | Aproximaciones por histograma acotado de 2048 cubetas. Informan el límite superior de la cubeta, con resolución relativa de hasta aproximadamente 3.125%; no guardan una muestra por operación. |
| Colecciones y tiempo de GC | Diferencias de los contadores de la JVM alrededor de la fase medida. |
| Heap antes y después | Instantáneas de memoria usada; no equivalen a bytes asignados por operación ni a memoria máxima alcanzada. |
| JVM, Java, sistema, arquitectura, procesadores y heap máximo | Contexto para decidir si dos ejecuciones son comparables. |

El reporte conserva carga, hilos, duración solicitada, calentamiento, modo, umbrales y fecha UTC. No guarda payloads ni valores de campos. El calentamiento no se suma a las métricas finales.

La carga es cerrada: cada hilo inicia su siguiente operación al terminar la anterior. Los percentiles describen el servicio observado bajo esa concurrencia; no incluyen espera de una cola externa ni corrigen omisión coordinada. Es una medición de carga instrumentada, no un microbenchmark aislado ni una prueba de transporte TCP.

## Automatizaciones

Para ejecutar una matriz local con Python 3, sin paquetes adicionales:

```sh
python3 scripts/medir-estres.py --hilos 1,4 --segundos 10 --calentamiento 2 \
  --repeticiones 3 --salida target/estres/comparacion-local
```

El script descubre las cargas disponibles en el JAR y abre un proceso JVM independiente por carga, concurrencia y repetición. Guarda cada medición, un `resumen.json` y `resumen.csv` con medianas, y `ejecucion.json` con revisión Git, indicador de cambios sin commit y parámetros. Usa `--cargas basica,especiales` para seleccionar un subconjunto o `--java` para elegir el ejecutable. Un error detiene la matriz con salida distinta de cero y conserva los reportes ya generados. El workflow manual utiliza este mismo script.

- **Cada push y pull request:** `Verificar Trama8583` ejecuta pruebas, catálogo funcional y una carga mixta de dos segundos con dos hilos y un segundo de calentamiento. Conserva JSON y CSV como artefacto `medicion-carga-ci` durante 30 días. Esta comprobación breve detecta errores concurrentes; su throughput es orientativo.
- **Bajo demanda:** en GitHub Actions abre `Medir estrés de Trama8583` y elige **Run workflow**. Ejecuta las seis cargas con uno y cuatro hilos, tres procesos JVM independientes por combinación por defecto, calentamiento de dos segundos y medición de diez segundos. Puedes ajustar duración, calentamiento y repeticiones. Los reportes quedan en artefactos separados por número de hilos.

El flujo manual limita a 3000 segundos la suma de fases por trabajo y a 60 minutos su ejecución completa. Los procesos usan heap fijo de 256 MiB. La fase breve de CI usa máximo de 256 MiB. Las automatizaciones quedan definidas en el repositorio; su ejecución remota requiere que esos cambios estén en GitHub.

## Comparar cambios y fijar umbrales

Para una comparación útil, ejecuta la revisión base y la revisión nueva con el mismo Java, hardware, heap, carga, calentamiento y concurrencia. Realiza al menos tres procesos independientes por combinación y compara la mediana de throughput y la distribución de los p95/p99. Conserva junto a los reportes el commit de cada revisión. Los runners compartidos pueden variar entre ejecuciones; evita decidir una regresión únicamente por una ejecución breve de CI.

Después de obtener una referencia en un entorno controlado, puedes activar límites:

```sh
# Ejemplo de mecanismo: sustituye estos valores por los de tu referencia medida.
"$JAVA_HOME/bin/java" -Xms256m -Xmx256m \
  -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar \
  --estres --carga basica --hilos 4 --calentamiento 5 --segundos 30 \
  --min-ops 1000 --max-p99-ms 20 --salida target/estres/comparacion.json
```

Los umbrales de rendimiento están desactivados por defecto. Un mínimo de operaciones por segundo incumplido, un p99 por encima del máximo, cero operaciones o cualquier error inesperado produce salida `1`; los reportes incluyen `cumple: false`. Los argumentos inválidos producen salida `2` antes de iniciar carga. El modo por conteo tiene un límite de espera de diez minutos; el modo temporal añade 15 segundos de margen para completar los trabajadores.

## Agregar una carga

Define el perfil y sus vectores independientes en `CargasEstres`, registra un nombre y agrega su prueba parametrizada. El script local y el workflow manual descubren esa lista automáticamente. Revisa el presupuesto de tiempo del workflow al agregar cargas y actualiza esta guía. Los cambios deliberados en formato requieren revisar los vectores; no generes los bytes esperados llamando al mismo codec que estás midiendo.
