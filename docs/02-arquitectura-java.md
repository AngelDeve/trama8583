# Arquitectura Java y selección del motor

## Decisión aceptada

Se implementará un motor propio Java con cero dependencias externas de ejecución en el núcleo y Apache 2.0 como licencia objetivo. El alcance inicial seguirá limitado al codec configurable; no incluye infraestructura transaccional ni criptografía. La [evaluación comparativa](09-evaluacion-motor-propio.md) desarrolla beneficios, costes y condiciones de esta elección.

j8583 ahorraría implementar primitivas de mensajería. Sin embargo, las combinaciones de prefijos, encodings, rellenos y reglas del contrato requieren control preciso. Un núcleo propio evita una segunda traducción de modelos y permite fijar sus límites y diagnósticos. Eso implica asumir la corrección y mantenimiento del parser: no se afirma que resulte más rápido, seguro o barato sin medirlo.

Los motores existentes podrán servir como comparadores externos de vectores equivalentes. No habrá dependencias sobre j8583 o jPOS en el artefacto principal, ni se copiará su implementación. Las referencias documentales no sustituyen vectores ni normas autorizadas.

## JDK y herramientas

Se propone Java 21 y Maven wrapper. El núcleo utilizará sólo APIs del JDK y podrá limitarse al módulo `java.base`. Arrays, colecciones, `ByteBuffer` y `Charset` cubren las utilidades generales; el cursor acotado, bitmap, BCD y prefijos ISO se implementarán como primitivas propias. Cada página de códigos adicional se comprobará en la JVM soportada.

JUnit, herramientas de propiedades y JMH podrán usarse únicamente en test o desarrollo. No serán dependencias del consumidor. La configuración inicial será mediante builder Java; cargar JSON con una biblioteca existente será una opción posterior en un artefacto independiente. No desarrollar un parser JSON genérico para evitar esa dependencia opcional.

El núcleo devuelve diagnósticos y no requiere SLF4J ni un backend de logging. No requiere Spring, Jackson, Netty ni una librería criptográfica. Las versiones de herramientas se fijarán y las de ejecución del núcleo deben permanecer vacías.

## Módulos propuestos

| Módulo | Responsabilidad |
| --- | --- |
| iso8583-core | Modelo público, builders de perfiles, compilación, codecs y reglas iniciales; sólo JDK |
| iso8583-testkit | Vectores sintéticos y comparadores de bytes; desarrollo y pruebas |
| iso8583-profile-json | Cargador opcional futuro; puede incorporar dependencias sólo para quien lo use |
| iso8583-cli | Inspección local posterior al MVP, con salida redactada |

Empezar con un único artefacto de runtime evita repartir prematuramente la API entre módulos. Ningún módulo opcional debe ser dependencia transitiva del núcleo. Las extensiones de campo se registrarán programáticamente mediante interfaces propias.

## Flujo de procesamiento

La aplicación entrega una trama delimitada al adaptador de framing. Éste entrega un payload al parser del sobre. El codec interpreta MTI, bitmap y campos según un perfil compilado. La validación estructural se realiza durante el parsing; la semántica se aplica después. Los resultados se devuelven a la aplicación para su decisión.

El framing, el sobre, el codec y las reglas son capas distintas. Así un cambio de prefijo TCP no altera los formatos de campos y un cambio de terminal no modifica el transporte.

## Contrato público propuesto

```java
PerfilCompilado perfil = constructorPerfil.construir().compilar();
CodecIso codec = codecs.paraPerfil(perfil);
ResultadoDecodificacion resultado = codec.decodificar(datos, OpcionesDecodificacion.estrictas());
ResultadoValidacion validacion = validador.validar(resultado.mensaje(), contexto);
byte[] salida = codec.codificar(mensaje, OpcionesCodificacion.canonicas());
```

Estas clases aún no existen. La API propia usará nombres en español según el [requisito de tropicalización](11-espanol-latinoamerica.md). `MensajeIso` contendrá MTI como cadena, sobre y mapa ordenado de campos. `ValorCampo` distinguirá texto, bytes y compuesto. Perfil y mensaje serán inmutables, con copias defensivas de arrays. Los codecs compartidos serán seguros para concurrencia; los cursores y buffers mutables pertenecerán a cada operación.

`ResultadoDecodificacion` incluirá bytes consumidos, perfil y versión efectiva, campos y diagnósticos. Un error contendrá código estable, ruta de campo, posición y longitud esperada/disponible, sin contenido. El catálogo definitivo separará los códigos programáticos del texto del diagnóstico, que estará en español.

Se distinguirá reencode canónico de reproducción exacta. La igualdad byte por byte requiere un perfil canónico o conservar explícitamente la representación original. El núcleo no retendrá toda la trama por defecto.
