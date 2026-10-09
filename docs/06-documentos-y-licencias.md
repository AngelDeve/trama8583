# Licencia permisiva y documentación de consulta local

## Acuerdo de alcance

El proyecto será una biblioteca Java flexible para codificar, decodificar y validar mensajes mediante perfiles. Actualmente no se dispone de manuales más recientes, definiciones de mantenimiento, gramáticas privadas ni contratos HSM, PIN o MAC. Esos recursos no serán requisitos de entrada para desarrollar el núcleo.

La documentación disponible orienta necesidades y preguntas; los ejemplos públicos seguirán siendo sintéticos. No se anuncia certificación ni compatibilidad completa con una red. Los formatos de tokens no documentados podrán transportarse como bytes opacos si el perfil define el límite externo; no se interpretarán por conjetura.

## Licencia objetivo Apache 2.0

Se recomienda Apache 2.0 para el código propio, ejemplos y documentación original que se decida publicar. Permite uso, modificación y distribución, incluidos productos comerciales, sin exigir publicar el código de la aplicación consumidora. Exige preservar la licencia y avisos aplicables al distribuir; contiene también una concesión de patentes bajo sus condiciones. Véase el [texto oficial de Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).

La licencia de la biblioteca no obliga a distribuir normas compradas ni manuales externos. Tampoco concede derechos sobre ellos. Los perfiles creados por un integrador pueden mantenerse privados: deben respetar por separado las condiciones de sus fuentes y dependencias.

No se convierte documentación ajena en Apache 2.0 por guardarla junto al proyecto. El archivo LICENSE se incorporará al formalizar la licencia del proyecto, al formalizar la distribución del código original y revisar cualquier material de terceros.

## Motor Java propio

Se acordó desarrollar el núcleo con sólo el JDK. No se incluirá un motor ISO externo ni se copiará su código. Las bibliotecas externas podrán utilizarse en pruebas o en módulos optativos con revisión de sus términos, sin convertirse en dependencias de ejecución del núcleo.

j8583 se conserva como alternativa evaluada, no como dependencia seleccionada. La [comparación](09-evaluacion-motor-propio.md) explica su ahorro potencial y el trabajo que exige un codec propio. La independencia de dependencias no cambia los derechos sobre normas, manuales ni perfiles derivados.

## Normas adquiridas para consulta

Se puede planificar comprar la edición ISO pertinente y consultarla localmente bajo los términos de acceso adquiridos. La publicación resultante incluirá código original y configuración publicable, sin incluir el PDF comprado, copias de tablas ni extractos extensos. Comprar la norma no concede automáticamente derechos de redistribución ni de uso con herramientas de IA: verificar esos términos antes de proporcionarla a una herramienta. Fuentes: [licencia de publicaciones ISO](https://www.iso.org/terms-conditions-licence-agreement.html) y [copyright ISO](https://www.iso.org/copyright.html).

La primera implementación se orienta a mensajes de estilo 1987. Una futura compra debe corresponder al objetivo que se quiera contrastar; ISO 8583:2023 no reemplaza por sí sola la especificación histórica de una interfaz. No hay una compra obligatoria inmediata para trabajar en el núcleo sintético y configurable.

## Separación de archivos

Guardar normas y manuales en una carpeta externa al checkout, con acceso acorde con su licencia. El repositorio contendrá sólo documentación original, ejemplos sintéticos y perfiles con derechos de publicación. No guardar documentos restringidos en recursos del JAR, fixtures, adjuntos de issues o artefactos de CI.

Como defensa adicional, `.gitignore` excluye carpetas locales designadas para material privado. No constituye un control de acceso y no protege archivos que ya estén versionados. Antes de publicar, revisar también recursos empaquetados y archivos de configuración. No se copió el PDF aportado al proyecto.

## Cuándo pedir información adicional

| Objetivo futuro | Información requerida | Efecto sobre el trabajo actual |
| --- | --- | --- |
| Acreditar conformidad con una edición | Norma y definiciones pertinentes con acceso autorizado | No bloquea el codec de laboratorio |
| Publicar un perfil real de integración | Formato confirmado, vectores y derechos de distribución | El integrador puede trabajar con su perfil privado |
| Interpretar tokens o subcampos particulares | Gramática exacta aportada por el integrador | Permitir contenido opaco delimitado mientras tanto |
| Verificar MAC o procesar PIN | Contrato criptográfico y proveedor/HSM correspondiente | El núcleo sólo transporta valores sensibles |
| Homologar una contraparte | Procedimiento y casos de certificación | Actividad posterior y específica |

El integrador no tendrá que modificar el código para cambiar longitudes, prefijos, encodings, cabeceras y reglas declarativas admitidas. Se ofrecerán extensiones Java para composiciones que excedan el contrato. Cada perfil debe definir los límites de los campos para que el parser pueda localizar los siguientes.
