# Escenarios ejecutables de Trama8583

Este módulo es un catálogo de ejemplos comprobables para aprender la API y validar cambios de la biblioteca. Cada escenario describe **Dado / Cuando / Entonces**, crea sus datos sintéticos y verifica el resultado. Puedes ejecutar uno sin preparar otro antes.

## Ejecutar

Con `JAVA_HOME` apuntando a un JDK 21, desde la raíz de Trama8583:

```sh
# Compilar el catálogo y sus dependencias; ejecutar también sus pruebas
./mvnw --batch-mode --no-transfer-progress -pl trama8583-escenarios -am verify

# Consultar casos disponibles
"$JAVA_HOME/bin/java" -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar --listar

# Ejecutar todos los escenarios
"$JAVA_HOME/bin/java" -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar --todos

# Ejecutar únicamente los casos elegidos, en el orden indicado
"$JAVA_HOME/bin/java" -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar NUC-01 ERR-04 SPR-01
```

En Windows usa `mvnw.cmd` y `java` de tu JDK 21. En IntelliJ ejecuta `EjecutarEscenarios.main` con los mismos argumentos o `CatalogoEscenariosTest` para ver cada caso como prueba individual.

El JAR incluye las dependencias de ejecución: después de compilarlo puedes copiarlo a otra carpeta o equipo con Java 21 o posterior y ejecutarlo con `java -jar`. No necesita Maven, JUnit, conexión de red, archivos de perfil, base de datos ni servicios externos en ejecución. La primera compilación sí necesita descargar Maven y dependencias si no están en caché. Los escenarios Spring abren y cierran su propio contexto local; los escenarios del núcleo no crean contextos Spring.

Sin argumentos se ejecutan todos los casos. `--ayuda` muestra el uso. Cada caso imprime su contrato y `CORRECTO` si lo cumple. Un rechazo esperado de los casos `ERR` también es correcto cuando coinciden código, campo y posición.

| Código de salida | Significado |
| --- | --- |
| `0` | Todos los casos seleccionados pasaron, o se consultó la ayuda/lista. |
| `1` | Falló al menos un caso; se muestra su ID y diagnóstico, y se completan los demás. |
| `2` | Argumentos o ID desconocidos; no se ejecuta ningún caso. |

## Catálogo

| ID | Qué puedes aprender o validar | Resultado clave |
| --- | --- | --- |
| NUC-01 | Crear un perfil, construir una solicitud y leer una trama conocida | Vector de 40 bytes, orden de campos y ceros conservados. |
| NUC-02 | Copiar una solicitud para construir una respuesta sintética | Vector de 42 bytes y solicitud intacta. |
| NUC-03 | Configurar texto LLVAR y LLLVAR | Prefijos decimales `09` y `015`, contenido conservado. |
| NUC-04 | Definir un campo binario 128 | Bitmap secundario y bytes opacos `00 7F 80 FF`. |
| NUC-05 | Elegir bitmap binario | Solicitud de 32 bytes con bitmap de ocho bytes. |
| NUC-06 | Incorporar un prefijo constante y una cabecera opaca | Sobre `LAB` + `01 02 03` antes del MTI. |
| NUC-07 | Reemplazar explícitamente una definición del perfil | Terminales de ocho y 16 bytes en perfiles independientes. |
| NUC-08 | Exigir siempre el bitmap secundario | Bit 1 activo y secundario vacío presente. |
| NUC-09 | Reutilizar mensajes y arreglos con seguridad | Las modificaciones no alteran la instantánea original. |
| ERR-01 | Omitir un obligatorio | `CAMPO_OBLIGATORIO_AUSENTE`, campo 41, posición -1. |
| ERR-02 | Exceder la longitud fija | `LONGITUD_INVALIDA`, campo 41, posición -1. |
| ERR-03 | Introducir letras en un campo numérico | `CODIFICACION_INVALIDA`, campo 3, posición -1. |
| ERR-04 | Leer una trama truncada | `TRAMA_INCOMPLETA`, campo 41, posición 32. |
| ERR-05 | Leer contenido después del último campo | `DATOS_SOBRANTES`, sin campo, posición 40. |
| ERR-06 | Enviar un campo sin definición | `CAMPO_NO_DEFINIDO`, campo 48, posición -1. |
| ERR-07 | Anunciar un LLVAR mayor al máximo del perfil | `LONGITUD_INVALIDA`, campo 48, posición 20. |
| ERR-08 | Superar el límite total de bytes | `LIMITE_EXCEDIDO` tanto al construir como al leer. |
| ESP-01 | Separar versión y estado de cabecera del resultado del cuerpo | Sobre sintético `DEMOV1E1` conservado independientemente del campo 39. |
| ESP-02 | Usar montos fijos o variables con signo | Perfiles explícitos; el tipo numérico rechaza signos. |
| ESP-03 | Transportar referencias ficticias de origen y destino | Campos 102/103 LLVAR independientes y bitmap secundario. |
| ESP-04 | Comparar eco entre solicitud y respuesta | La regla de aplicación detecta folio o terminal ausentes o modificados. |
| ESP-05 | Condicionar la presencia de un campo por operación y resultado | Reglas de ejemplo separadas de la validación estructural. |
| ESP-06 | Distinguir prefijo externo de longitudes internas | Campo 48 LLLVAR y gramática inventada con dos tokens. |
| ESP-07 | Conservar un token desconocido con bytes no ASCII | Campo 62 binario, contenido opaco preservado. |
| ESP-08 | Rechazar contenedores internos inválidos | Longitudes, duplicados y límites verificados por el parser de ejemplo. |
| ESP-09 | Representar mensajes de reverso y de red | MTI 0400/0410/0800/0810 con vectores independientes. |
| ESP-10 | Preparar explícitamente espacios en un texto fijo | La aplicación rellena; el codec conserva y rechaza longitudes incorrectas. |
| SPR-01 | Inyectar y utilizar `PasarelaIso` con Spring sin Boot | Solicitud y respuesta coinciden con vectores conocidos. |
| SPR-02 | Usar canales y conservar cabeceras de aplicación | `folioLocal` y contenido conservados en ambos flujos. |
| SPR-03 | Manejar un error y continuar usando la pasarela | Diagnóstico ISO disponible en la causa y siguiente operación válida. |

La posición es absoluta desde cero; `-1` indica que no hay una posición disponible en una trama de entrada. Los valores son de laboratorio. El campo 128 del ejemplo transporta bytes opacos; no calcula ni valida un MAC. Una respuesta con campo 39 `00` es una construcción sintética, no una autorización. La familia ESP usa reglas y una gramática inventadas de aplicación: no agrega un motor de reglas ni un parser genérico de compuestos al núcleo. Consulta [EscenariosEspeciales.java](src/main/java/org/gca/trama8583/escenarios/EscenariosEspeciales.java), [ReglasEjemplo.java](src/main/java/org/gca/trama8583/escenarios/ReglasEjemplo.java) y [ContenedorEjemplo.java](src/main/java/org/gca/trama8583/escenarios/ContenedorEjemplo.java).

## Ejecutar estrés

El mismo JAR ofrece seis cargas con mediciones y reportes JSON/CSV:

```sh
"$JAVA_HOME/bin/java" -Xms256m -Xmx256m \
  -jar trama8583-escenarios/target/trama8583-escenarios-1.0-SNAPSHOT.jar \
  --estres --carga mixta --hilos 4 --calentamiento 2 --segundos 10 \
  --salida target/estres/mixta-4h.json
```

La [guía de estrés](../docs/15-pruebas-estres.md) explica qué mide una operación, los percentiles aproximados, los umbrales opcionales, las repeticiones y las automatizaciones en GitHub Actions.

## Leer y ampliar los ejemplos

Empieza por [EscenariosNucleo.java](src/main/java/org/gca/trama8583/escenarios/EscenariosNucleo.java) y su método `solicitudConocida`. Las otras recetas están en [EscenariosErrores.java](src/main/java/org/gca/trama8583/escenarios/EscenariosErrores.java) y [EscenariosSpring.java](src/main/java/org/gca/trama8583/escenarios/EscenariosSpring.java). [DatosLaboratorio.java](src/main/java/org/gca/trama8583/escenarios/DatosLaboratorio.java) contiene el perfil común y los vectores literales, sin estado mutable compartido.

Para agregar un caso:

1. Crea un método que prepare sus datos, invoque la API pública y compruebe un resultado explícito. Usa perfiles nuevos y cierra los recursos con `try`.
2. Registra un `Escenario` en la familia correspondiente con un ID nuevo y un contrato Dado / Cuando / Entonces. Mantén los IDs existentes para conservar comandos y referencias.
3. Para codificación, define bytes esperados independientes del codec. Para rechazos, comprueba código, campo y posición. Una ida y vuelta por sí sola puede ocultar un error compartido entre codificación y decodificación.
4. Actualiza esta tabla y ejecuta `verify` y el JAR. El caso registrado se incorpora automáticamente tanto a consola como a JUnit; no copies la comprobación en otra prueba.

`Comprobaciones` verifica resultados con excepciones `AssertionError`; no requiere activar `-ea`. JUnit solo existe en alcance `test` y envuelve las mismas acciones del catálogo. El empaquetado reutiliza el plugin Maven de Boot del proyecto para distribuir las dependencias en un solo JAR.

## Validación de mantenimiento

`./mvnw verify` desde la raíz ejecuta las pruebas técnicas existentes y este catálogo. GitHub Actions también ejecuta el JAR después de empaquetarlo, para comprobar la distribución autónoma. Los reportes JUnit quedan en `trama8583-escenarios/target/surefire-reports/`, con ID y título de cada escenario.

Al cambiar deliberadamente un contrato público, revisa el escenario y su vector esperado junto con la implementación. Estos ejemplos cubren el subconjunto de laboratorio.

## Escenarios contra el servidor TCP

`EscenariosTcp` conserva los vectores y comprobaciones de extremo a extremo dentro de la biblioteca. El **Banco de pruebas** los ejecuta desde **Servidor TCP** contra el [ejemplo Spring Boot](https://github.com/AngelDeve/trama8583-ejemplo-spring-tcp) en ejecución.

El escenario financiero prueba `0200 → 0210` y el de red `0800 → 0810`. Ambos verifican byte a byte la respuesta, incluido el prefijo de longitud de dos bytes big endian y el campo 39 con valor sintético `00`. El catálogo autónomo de 30 escenarios y la carga de estrés siguen ejecutándose por separado.
