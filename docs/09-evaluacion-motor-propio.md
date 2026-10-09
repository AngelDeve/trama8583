# Evaluación de j8583 frente a un motor propio

## Recomendación para este proyecto

El mantenedor aceptó el 9 de octubre de 2026 un núcleo Java propio sin dependencias externas de ejecución, con Apache 2.0 como licencia objetivo. La decisión responde al alcance actual: procesar bytes mediante perfiles explícitos, ofrecer extensibilidad y conservar una integración ligera. No incluye un switch, red, autorización, criptografía ni soporte simultáneo de todas las ediciones.

Es una decisión de diseño sustentada en documentación y alcance, no en un benchmark. Todavía no se han ejecutado j8583 ni un codec propio. La ventaja de un motor propio es el control del contrato y sus dependencias; su principal coste es asumir la implementación, prueba y mantenimiento de cada regla binaria.

## Qué aporta j8583

La [introducción del mantenedor](https://j8583.sourceforge.net/) describe generación y lectura de mensajes, modelo de campos y tipos comunes, además de extensiones para codificar objetos particulares. La [guía de uso](https://j8583.sourceforge.net/guide.html) describe bitmaps secundarios, cabeceras, plantillas de creación y parsing, representación ASCII/binaria y configuración XML.

Eso puede ahorrar trabajo en un primer codec y permite concentrarse antes en reglas y perfiles. Sin embargo, esas páginas están rotuladas como documentación de 1.10.2 y publicadas en 2019: no prueban todos los comportamientos de 3.0.0. El [POM de net.sf.j8583:j8583:3.0.0](https://central.sonatype.com/artifact/net.sf.j8583/j8583/3.0.0) declara Apache 2.0 e incluye SLF4J API 1.7.36 como dependencia de compilación. Por ello no cumple literalmente la meta de un runtime basado exclusivamente en el JDK.

No se ha demostrado que j8583 admita sin extensiones toda combinación de encoding, unidad de longitud, relleno y prefijo de nuestro contrato. Un adaptador deberá comprobarlo. Tampoco garantiza conformidad con una red ni elimina la necesidad de definir sus particularidades.

## Comparación

| Criterio | Reutilizar j8583 | Núcleo propio |
| --- | --- | --- |
| Primer codec funcional | Menos primitivas por implementar | Hay que implementar primitivas y pruebas |
| Dependencias de ejecución | Motor y dependencias declaradas | Sólo el JDK si se mantiene el alcance |
| Contrato de perfiles | Traducción al modelo del motor | Definido directamente por el proyecto |
| Particularidades | Extensiones dentro de lo que admita el motor | Encodings, prefijos y reglas extensibles desde el diseño |
| Diagnósticos y límites | Adaptar y comprobar errores del motor | Control completo; responsabilidad completa |
| Mantenimiento | Cambios propios y actualizaciones del proveedor | Todos los fallos del codec recaen en el proyecto |
| Licencias | Revisar artefacto, fuentes y avisos | Código original y licencia del proyecto |
| Rendimiento | Debe medirse para nuestro perfil | Debe medirse; menos dependencias no asegura mayor velocidad |
| Soporte normativo | Necesita documentación y pruebas específicas | Necesita la misma documentación y pruebas |

No se asignan plazos ni porcentajes de ahorro sin implementar un corte vertical. Para este objetivo, la flexibilidad de perfiles es parte central del producto y una fachada sobre otro modelo podría introducir complejidad propia. Si la prioridad cambiase a entregar una integración urgente de formato convencional, reutilizar un motor volvería a ser una opción razonable.

## Qué significa sin dependencias

El artefacto principal no tendrá dependencias Maven de compile/runtime fuera del JDK. Herramientas de build y bibliotecas de test pueden utilizarse en el desarrollo sin formar parte del JAR entregado al consumidor. Si también se prohíben en pruebas, se reduce la comodidad de validación, sin reducir dependencias del producto.

El núcleo se limitará a `java.base`: arrays, colecciones, `ByteBuffer`, `Charset`, `BigDecimal` cuando se necesite una vista de importe, y utilidades estándar. La lectura binaria y orden de bytes se apoyan en [ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html). Los [charsets](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html) permiten comprobar páginas disponibles en la JVM; no se promete cualquier EBCDIC en toda distribución. ASCII se validará estrictamente y BCD se implementará como codec explícito de nibbles.

No se incluirán Spring, Jackson, SLF4J, motor externo ni proveedor criptográfico en el núcleo. Éste devolverá diagnósticos redactados a la aplicación; no necesitará un framework de logging. Las extensiones serán objetos registrados por el integrador, sin clases ejecutables indicadas en archivos no confiables.

## Configuración sin escribir otro parser

La primera versión tendrá un builder Java para perfiles, campos y reglas. Esto permite cambiar longitudes, cabeceras y formatos sin modificar el código de la biblioteca, aunque el integrador compile su configuración. Los perfiles compilados serán inmutables.

Los JSON actuales son ejemplos del contrato lógico. Se conservarán para diseño y vectores, pero no serán una promesa de carga nativa en el núcleo. Un futuro módulo `iso8583-profile-json` podrá usar una biblioteca JSON existente y será optativo, sin dependencia inversa desde el núcleo. Para consumidores que exijan cero dependencias también en configuración, se podrá estudiar un formato limitado basado en utilidades del JDK después del builder; no escribir un parser JSON o YAML genérico en este proyecto.

## Trabajo que hay que asumir

1. Modelo y perfiles: mensajes inmutables, definiciones de campo, reglas, capacidades y validación de conflictos.
2. Cursor de lectura: límites por operación, offsets, control de truncamiento y aritmética sin desbordamiento.
3. Bitmaps: representación binaria y ASCII hex, bloques de 64/128 bits y generación consistente.
4. Encodings: texto estricto, bytes opacos, BCD, rellenos y conversión separada de prefijos.
5. Longitudes: fijo y variable, bytes/nibbles, prefijos ASCII, BCD e integer binario.
6. Sobre y compuestos: límites externos explícitos, estrategias extensibles y profundidad acotada.
7. Validación: formato estructural separado de reglas semánticas y relaciones entre mensajes.
8. Evidencia: vectores independientes, pruebas negativas, propiedades, fuzzing y concurrencia.

El mayor riesgo no es convertir un bitmap: es interpretar longitudes o rellenos de forma incorrecta y desplazar todos los campos posteriores. Las pruebas de ida y vuelta no bastan; encoder y decoder propios pueden compartir el mismo error. Se requieren bytes esperados independientes y comparación con otras implementaciones para subconjuntos equivalentes.

## Corte vertical antes de ampliar

Construir primero MTI ASCII, bitmap ASCII hex de 64/128 bits, campos fijos, LLVAR ASCII y sobre configurable. Verificar los tres vectores existentes y añadir fronteras de longitud, campos variables y truncamiento en cada offset. Sólo después agregar BCD, bitmap binario, EBCDIC y compuestos con vectores propios.

Aceptar el corte cuando el JAR tenga cero dependencias externas de ejecución, los perfiles se construyan con la API, los bytes coincidan con vectores independientes y cada fallo devuelva un error controlado sin datos sensibles. Ese resultado permite estimar el resto con evidencia. Si revela un coste de mantenimiento excesivo, documentar el motivo antes de reconsiderar un motor externo.
