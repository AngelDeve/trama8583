# Español para Latinoamérica

Estado: requisito aceptado por el mantenedor el 9 de octubre de 2026.

## Idioma principal

La biblioteca estará tropicalizada para desarrolladores de Latinoamérica. El español será el idioma principal de la documentación, guías de instalación, Javadoc, ejemplos, ayuda de herramientas y mensajes de diagnóstico. Se usará español claro y neutral, evitando regionalismos que dificulten su comprensión en otros países.

Los términos técnicos tendrán una explicación en español en su primera aparición. Se conservarán las siglas del protocolo, como ISO 8583, MTI, BCD, LLVAR, PIN y MAC, con un glosario que explique su significado. Para bitmap se utilizará también “mapa de bits”; para codec, “codificador y decodificador”.

## API y configuración

Los nombres propios de clases y métodos públicos serán descriptivos en español, sin tildes ni eñes en identificadores. Por ejemplo: `MensajeIso`, `PerfilMensaje`, `ConstructorPerfil`, `codificar`, `decodificar` y `validar`. Se respetarán las convenciones de nombres de Java. Los tipos y métodos del JDK conservan sus nombres originales.

Las futuras propiedades públicas de configuración y su ayuda se definirán de forma coherente con este criterio. Los JSON existentes son propuestas previas del contrato, con claves en inglés: deberán alinearse antes de publicar el primer esquema estable. No mantener dos conjuntos de nombres equivalentes sin una necesidad concreta.

## Código interno

El español también será el idioma preferido del código interno: clases, interfaces, métodos, variables, constantes, paquetes, nombres de pruebas y comentarios. Se usarán nombres claros como `longitudMaxima`, `camposPresentes`, `posicionActual` y `validarCampoObligatorio`. Los comentarios explicarán decisiones o restricciones que el código por sí solo no haga evidentes.

Los identificadores propios evitarán tildes y eñes para facilitar el uso de herramientas. Se conservarán los nombres impuestos por Java, el JDK, interfaces externas y formatos de intercambio, así como siglas y términos establecidos como MTI, PIN, MAC, ASCII, BCD y bitmap. Las excepciones tendrán una razón técnica o de interoperabilidad; no se traducirán bytes, etiquetas ni valores definidos por un protocolo.

Durante la revisión de código se comprobará la coherencia del vocabulario. Se evitarán mezclas innecesarias como `obtenerFieldLength` cuando corresponda `obtenerLongitudCampo`.

## Diagnósticos

Los errores tendrán un código estable para uso programático y una explicación en español. La aplicación no debe comparar el texto traducible para decidir qué hacer. Los mensajes indicarán campo, posición y longitudes cuando corresponda, sin exponer contenido sensible.

Ejemplo de texto: “El campo 41 requiere 16 bytes; quedan 8 bytes disponibles en la posición 32”. El código del error será independiente de la redacción y se documentará en el catálogo.

## Particularidades regionales

Los ejemplos utilizarán datos sintéticos y explicaciones aplicables a Latinoamérica. Moneda, zona horaria, formato de fecha, exponente monetario y reglas de cada integración se configurarán explícitamente. El idioma de la biblioteca no determina esos valores ni modifica los bytes del protocolo.

Los textos de documentación pueden contener acentos y caracteres Unicode. Los valores de las tramas deben seguir la codificación del perfil; no se introducirán caracteres españoles en una trama ASCII por traducir su ayuda.

## Verificación antes de publicar

Revisar que las funciones públicas tengan Javadoc y ejemplos en español, que los diagnósticos sean comprensibles sin conocer inglés y que el glosario explique las siglas utilizadas. Verificar que las diferencias regionales permanezcan configurables y que el contrato de configuración tenga un vocabulario consistente.
