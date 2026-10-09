# Trama8583

Biblioteca Java para construir, interpretar y validar mensajes ISO 8583 mediante perfiles configurables. Núcleo propio sin dependencias externas de ejecución, pensado para desarrolladores de Latinoamérica.

> **Estado: preparación del proyecto.** Este repositorio contiene la definición inicial del componente; todavía no incluye una implementación ejecutable ni una versión publicada.

## Propósito

Ofrecer una API clara para trabajar con la estructura de un mensaje: indicador de tipo de mensaje (MTI), bitmap y campos de datos. El formato de cada campo deberá definirse mediante un perfil de mensajería explícito.

El idioma principal será español para Latinoamérica: documentación, Javadoc, ejemplos, API propia, código interno y diagnósticos, en la medida que permitan Java y los protocolos. Las particularidades regionales se configurarán por perfil. Véase el [requisito de tropicalización](docs/11-espanol-latinoamerica.md).

## Alcance previsto

- Representar mensajes y campos mediante una API Java.
- Codificar y decodificar mensajes según un perfil configurado.
- Validar campos obligatorios, longitudes y formatos.
- Informar errores de lectura y validación con contexto útil.
- Incluir ejemplos reproducibles y pruebas con datos sintéticos.

Estas capacidades son objetivos de desarrollo, no funcionalidades disponibles actualmente. La primera versión deberá documentar la edición de ISO 8583, las codificaciones y el perfil de campos que admite.

## Límites del componente

El proyecto se centra en el procesamiento de mensajes. La conectividad de red, el enrutamiento transaccional, la autorización de operaciones y la gestión de claves criptográficas quedan fuera del alcance inicial.

## Uso y compilación

El proyecto contiene una estructura Maven creada en IntelliJ IDEA y todavía no implementa el procesamiento de mensajes. El POM actual configura Java 27 y codificación UTF-8. Las instrucciones de uso se incorporarán con el primer codec; aún no hay una versión publicada. La documentación de diseño propone Java 21: la versión mínima definitiva se acordará antes de implementar el núcleo.

## Hoja de ruta

La investigación y el diseño para iniciar el desarrollo están en el [índice de documentación](docs/README.md). Incluyen arquitectura Java, evaluación de motores existentes, contrato de perfiles, requisitos ATM generalizados, pruebas, documentos oficiales necesarios y opciones de nombre.

El enfoque acordado es un núcleo Java propio, sin dependencias externas de ejecución, configurable mediante perfiles versionados. La licencia objetivo es Apache 2.0. La [evaluación de motor propio frente a j8583](docs/09-evaluacion-motor-propio.md) detalla beneficios, costes y validación necesaria. “Universal” significa extensible mediante perfiles explícitos; no se promete detectar cualquier formato automáticamente.

Los [ejemplos de laboratorio](examples/profiles/lab-ascii-v1.json) y [vectores sintéticos](examples/messages/vectors.json) son material de diseño, no una implementación disponible ni un perfil de producción.

1. Definir el perfil inicial y la versión mínima de Java.
2. Diseñar el modelo de mensajes y la API pública.
3. Implementar codificación, decodificación y validación.
4. Incorporar pruebas, ejemplos y compilación automática en GitHub Actions.
5. Publicar una primera versión con sus limitaciones documentadas.

## Contribuciones

Las propuestas deben describir el caso de uso, el perfil de mensajería y el comportamiento esperado. Los ejemplos y pruebas deberán utilizar datos sintéticos; evita incluir números de tarjeta reales, PIN, claves o mensajes de producción.

## Licencia

Se propone Apache 2.0 para el código y material original publicable; falta formalizarla en LICENSE y revisar los materiales publicables. Las normas adquiridas y manuales externos se consultarán localmente y permanecerán fuera del repositorio. Su ausencia actual no bloquea el desarrollo del núcleo configurable. Véase la [política de licencia y consulta local](docs/06-documentos-y-licencias.md).
