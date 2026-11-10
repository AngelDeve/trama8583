# Trama8583

Biblioteca Java para construir, interpretar y validar mensajes ISO 8583 mediante perfiles explícitos. Núcleo propio sin dependencias externas de ejecución, con canales de Spring Integration y una API de alto nivel en español para Latinoamérica.

**Estado: primera implementación de laboratorio, sin versión publicada.** El codec procesa un subconjunto de mensajes de estilo 1987. Los perfiles y vectores son sintéticos; la compatibilidad con una red o una edición completa requiere evidencia y pruebas específicas. “Universal” significa extensible mediante perfiles, sin detección automática del formato.

## Módulos

| Módulo | Función |
| --- | --- |
| `trama8583-nucleo` | Mensajes inmutables, perfiles y codec propio; solo JDK en ejecución. |
| `trama8583-spring-integracion` | Dos `DirectChannel`, flujos de transformación y `PasarelaIso`. |
| `trama8583-escenarios` | Catálogo autónomo de usos y errores esperados, ejecutable por consola y por JUnit. |

Java 21, Maven Wrapper 3.9.11 y versiones Spring administradas con el BOM de Spring Boot 4.1.1. La biblioteca de integración puede usarse con Spring Framework sin Boot; la demostración utiliza Boot.

## Compilar y ejecutar

Configura `JAVA_HOME` para tu JDK 21 y ejecuta desde el repositorio:

```sh
./mvnw --batch-mode --no-transfer-progress verify
```

En Windows utiliza `mvnw.cmd` y el ejecutable `java` de tu JDK 21. La primera compilación descarga Maven y dependencias. `./mvnw install` instala los módulos en tu repositorio Maven local; todavía no hay un artefacto publicado en Maven Central.

En IntelliJ abre el `pom.xml` raíz, elige **JDK 21**, **Maven Wrapper** y **Project JDK** para el importador y ejecutor Maven. Los ajustes personales del IDE permanecen fuera de Git. GitHub Actions verifica el proyecto con Temurin 21.

La suite JUnit reúne 405 casos del núcleo, canales Spring y catálogo ejecutable, con nombres en español. Consulta la [matriz de pruebas](docs/13-matriz-pruebas-junit.md) para ver el alcance, los comandos por módulo y los reportes.

## Ejemplo Spring Boot con TCP

El servidor de laboratorio vive en un repositorio independiente, consume la biblioteca desde Maven y escucha en `127.0.0.1:9000` con la configuración inicial. [Ejemplo Spring Boot con TCP](https://github.com/AngelDeve/trama8583-ejemplo-spring-tcp)

- En el Inspector, usa **Ejemplo Spring Boot · TCP** para construir y enviar mensajes manuales. [Inspector](https://github.com/AngelDeve/trama8583-inspector)
- En el Banco de pruebas, abre **Servidor TCP** para comprobar los intercambios `0200 → 0210` y `0800 → 0810`. [Banco de pruebas](https://github.com/AngelDeve/trama8583-banco-pruebas)

## Escenarios para aprender y validar cambios

El [catálogo de escenarios ejecutables](trama8583-escenarios/README.md) incluye 30 recetas independientes con **Dado / Cuando / Entonces**: solicitudes y respuestas, LLVAR/LLLVAR, campos binarios, bitmaps, variantes de perfil, errores, casos especiales de laboratorio y canales Spring. Cada receta prepara datos sintéticos y comprueba el resultado. Los mismos casos se ejecutan automáticamente con `verify`.

Después de compilar, puedes listar o ejecutar casos desde un JAR con todas sus dependencias:

```sh
"$JAVA_HOME/bin/java" -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar --listar
"$JAVA_HOME/bin/java" -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar NUC-01 ERR-04 SPR-01
```

Usa `--todos` para recorrer el catálogo completo. Cada caso informa su resultado; un fallo produce código de salida `1` para automatizar la validación. El JAR ya construido solo requiere Java 21 o posterior y funciona sin servicios externos.

## Pruebas de estrés

El JAR también permite carga concurrente con calentamiento, seis tipos de carga, conteos de errores y reportes JSON/CSV. Incluye operaciones por segundo, latencia mínima/media/máxima, p50/p95/p99 aproximados, heap y actividad de GC:

```sh
"$JAVA_HOME/bin/java" -Xms256m -Xmx256m \
  -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar \
  --estres --carga mixta --hilos 4 --calentamiento 2 --segundos 10 \
  --salida target/estres/mixta-4h.json
```

Cada push/PR ejecuta una carga breve; el workflow manual `Medir estrés de Trama8583` repite las seis cargas con uno y cuatro hilos y conserva artefactos de medición. Consulta la [guía de estrés](docs/15-pruebas-estres.md) para interpretar métricas y comparar cambios en condiciones equivalentes.

## Perfil y mensaje

```java
import org.gca.trama8583.nucleo.*;

var perfil = PerfilMensaje.crear("laboratorio-ascii-v1")
        .campo(DefinicionCampo.fijo(3, TipoDato.NUMERICO_ASCII, 6))
        .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
        .campo(DefinicionCampo.fijo(41, TipoDato.TEXTO_ASCII, 8))
        .obligatorios("0200", 3, 11, 41)
        .construir();
var solicitud = MensajeIso.crear("0200")
        .campo(3, "000000").campo(11, "000001").campo(41, "TERM0001")
        .construir();
CodecIso codec = new CodecIsoBasico(perfil);
byte[] trama = codec.codificar(solicitud);
MensajeIso interpretado = codec.decodificar(trama);
```

Cada campo debe estar definido en el perfil. El bitmap se genera automáticamente y los campos se escriben por número ascendente. `reemplazarCampo(...)` permite variaciones explícitas, por ejemplo una longitud de terminal de 16 bytes. Una definición duplicada mediante `campo(...)` se rechaza.

## Canales Spring

Importa `ConfiguracionIntegracionIso`, declara un único bean `PerfilMensaje` e inyecta `PasarelaIso`:

```java
byte[] trama = pasarela.construir(solicitud);
MensajeIso recibido = pasarela.interpretar(trama);
```

Los canales `trama8583.codificacion` y `trama8583.decodificacion` aceptan mensajes Spring con payload `MensajeIso` y `byte[]`, respectivamente. La pasarela gestiona la respuesta. Los flujos conservan cabeceras de aplicación y se ejecutan en el hilo del llamador. Consulta la [guía de integración](docs/12-integracion-spring.md).

## Capacidades actuales

- MTI de cuatro dígitos ASCII; bitmap primario y secundario en binario o hexadecimal ASCII en mayúsculas.
- Campos 2–128: numéricos ASCII, texto ASCII imprimible y bytes opacos.
- FIJA, LLVAR y LLLVAR con prefijos decimales ASCII y longitudes en bytes.
- Prefijo constante, cabecera opaca de longitud fija, secundario obligatorio opcional y límite total configurable (4096 bytes por defecto).
- Campos obligatorios por MTI; errores con código, campo y posición cuando aplica.
- Mensajes y perfiles inmutables, con copias defensivas; `toString()` omite valores de campos.

El codec rechaza campos desconocidos, codificaciones inválidas, longitudes incompatibles, truncamientos y bytes sobrantes. No agrega relleno ni recupera una trama adivinando límites.

El transporte TCP del laboratorio pertenece a la aplicación de ejemplo. Para el núcleo siguen pendientes el cargador JSON/YAML, BCD, EBCDIC, bitmap terciario, subcampos/TLV, reglas entre campos, correlación transaccional y PIN/MAC/HSM. El campo 65 se representa como dato definido explícitamente en este subconjunto. Un protocolo que lo use para anunciar otro bitmap requiere una extensión. Los [JSON de laboratorio](examples/profiles/lab-ascii-v1.json) siguen siendo ejemplos de diseño y no se cargan en ejecución.

## Documentación y contribuciones

El [índice de documentación](docs/README.md) reúne investigación, perfiles, pruebas y decisiones. Los documentos previos también describen capacidades futuras; esta lista y la guía Spring delimitan lo implementado. Documentación, Javadoc, API propia y diagnósticos siguen el [requisito de español para Latinoamérica](docs/11-espanol-latinoamerica.md).

Utiliza datos sintéticos en ejemplos y pruebas. Normas adquiridas, manuales externos y perfiles privados se consultan localmente y permanecen fuera del repositorio, por ejemplo en `local-private/`, que está ignorada por Git.

## Licencia

La licencia objetivo es Apache 2.0 para código y material original publicable; falta formalizarla en `LICENSE` antes de publicar una versión. Las fuentes externas conservan sus licencias. Consulta la [política de licencia y consulta local](docs/06-documentos-y-licencias.md).
