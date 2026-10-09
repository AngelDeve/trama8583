# Fuentes de investigación y límites de evidencia

Consulta: 9 de octubre de 2026. Las referencias siguientes son primarias: catálogo oficial, documentación de mantenedores y repositorios de proyectos. Las capacidades propuestas en arquitectura y perfiles son decisiones de diseño, no afirmaciones de la norma.

| Fuente | Uso en el proyecto | Límite |
| --- | --- | --- |
| [ISO 8583:2023](https://www.iso.org/standard/79451.html) | Edición vigente y alcance general | La ficha pública no contiene la especificación íntegra |
| [Agencias de mantenimiento ISO](https://www.iso.org/maintenance_agencies.html) | Canal oficial de mantenimiento y contacto | Verificar otra vez al iniciar una implementación normativa |
| [Copyright ISO](https://www.iso.org/copyright.html) | Derechos sobre publicaciones | Acceso público no equivale a redistribución |
| [Licencia de publicaciones ISO](https://www.iso.org/terms-conditions-licence-agreement.html) | Condiciones de consulta y acceso | Elegir licencia adecuada para las personas que consultan |
| [GenericPackager jPOS](https://jpos.org/docs/tutorial/iso-packager/generic-packager/) | Configuración de campos y reutilización Java | Los packagers incluidos no certifican cualquier contraparte |
| [Guía jPOS](https://jpos.org/doc/proguide.pdf) | Modelo de reutilización y licencia del proyecto | Verificar los términos del artefacto seleccionado |
| [Release jPOS 3.0.2](https://jpos.org/blog/jpos-jposee-3.0.2/) | Versión anunciada el 30 de septiembre de 2026 y Java 26 | No se ejecutó una prueba de esa versión |
| [POM publicado net.sf.j8583 3.0.0](https://central.sonatype.com/artifact/net.sf.j8583/j8583/3.0.0) | Declaración de licencia y dependencia SLF4J de la alternativa evaluada | Falta revisar JAR, fuentes y dependencias |
| [Introducción j8583](https://j8583.sourceforge.net/) | Capacidades generales del motor existente | Documentación antigua, rotulada 1.10.2 |
| [Guía j8583](https://j8583.sourceforge.net/guide.html) | Plantillas, formatos y extensiones | No prueba el comportamiento de 3.0.0 |
| [ByteBuffer Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html) | Utilidades de bytes del JDK | No implementa reglas ISO 8583 |
| [Charset Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html) | Disponibilidad y conversión de páginas de código | Comprobar cada encoding en la JVM objetivo |
| [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Licencia objetivo del proyecto | No concede derechos sobre documentos externos |
| [j8583 fork de Thibaud Ledent](https://github.com/thibaudledent/j8583) | Alternativa Java y declaración LGPL-3.0 | No extrapolar a otros forks o versiones |
| [POM j8583](https://github.com/thibaudledent/j8583/blob/main/pom.xml) | Referencia de toolchain release 17 | Rama mutable; validar tag antes de fijar dependencia |
| [Especificaciones pyiso8583](https://pyiso8583.readthedocs.io/en/latest/specifications.html) | Separación de encoding, longitud, prefijo y relleno | Ejemplos de implementación, no catálogo normativo |
| [moov-io/iso8583](https://github.com/moov-io/iso8583) | Implementación independiente para comparar vectores | Go; no candidato de runtime para esta biblioteca Java |

## Caso de estudio aportado

La guía local de ocho páginas se usó para generalizar necesidades de cabecera, formatos alternativos, compuestos y reglas de relación. Se leyeron las ocho páginas y se revisó visualmente el núcleo técnico de las páginas 3 a 5. Contiene referencias a secciones del manual original que no están incluidas en el archivo.

Su matriz y ejemplos requieren confirmación, en especial los errores señalados en [requisitos ATM](04-requisitos-atm.md). No se verificaron sus reglas operativas con una contraparte, no se copiaron cuestionarios ni catálogos completos, y no se distribuye el archivo fuente. No se atribuye valor normativo a sus afirmaciones sobre ISO.

## Evidencia pendiente

La norma histórica autorizada, el manual vigente, los derechos de publicar perfiles reales y los vectores de contraparte quedan pendientes para objetivos de conformidad o integración específicos. No son bloqueos del núcleo configurable. La recomendación del motor propio es documental: no se descargaron JARs ni se ejecutó un benchmark o prueba de interoperabilidad. Los vectores incluidos son de laboratorio y se verifican aritméticamente; todavía no existe codec Java.
