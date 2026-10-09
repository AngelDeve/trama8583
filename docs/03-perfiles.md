# Contrato de perfiles configurables

## Principio de configuración

Cada formato tendrá identificador, versión semántica y edición declarada. La compilación resolverá herencia y producirá un perfil inmutable con hash del contenido efectivo. Los mensajes procesados registrarán ese hash para reproducir diagnósticos.

Los archivos JSON de `examples/profiles` son propuestas del contrato v1, no configuraciones que ya pueda cargar una biblioteca. La primera implementación configurará perfiles mediante builder Java, usando este contrato lógico. El validador operará sobre el modelo; el esquema JSON y su cargador corresponden a un módulo opcional posterior.

## Elementos del contrato

| Propiedad | Semántica |
| --- | --- |
| schemaVersion | Versión del formato de configuración |
| id y version | Identidad del perfil y versión exacta |
| edition | Objetivo declarado, sin inferir conformidad |
| extends | Referencia local explícita y versión exacta del padre |
| framing | Delimitación fuera del mensaje; por defecto la aplicación entrega un payload completo |
| envelope | Literales y segmentos de cabecera, con orden y longitudes |
| mti | Encoding y longitud; reglas permitidas del perfil |
| bitmap | Binario o hexadecimal textual; bloques permitidos y política del secundario |
| fields | Mapa de definiciones de campo |
| rules | Presencia, prohibición, condiciones y relaciones |
| limits | Topes de bytes, campos y profundidad |

Un campo debe definir por separado tipo lógico, codificación de datos, unidad de longitud y codificación del prefijo. `N` no implica BCD. `LLVAR` no determina por sí solo el número de bytes del prefijo. La documentación de [pyiso8583](https://pyiso8583.readthedocs.io/en/latest/specifications.html) muestra explícitamente estas dimensiones.

## Longitudes y representación

`length.kind` será `fixed` o `variable`; `size` aplica a fijo, `max` a variable. `unit` será `bytes` o `nibbles` en v1. En texto ASCII cada carácter ocupa un byte. No se admitirá UTF-8 genérico en perfiles que supongan esa equivalencia. EBCDIC requiere una página de códigos explícita.

El prefijo variable define `encoding` (`ascii`, `bcd`, `binary-uint` o página EBCDIC), `widthBytes` y, para enteros binarios de varios bytes, endianness. No cuentan los bytes del prefijo en la longitud del contenido salvo una opción explícita revisada. Para BCD impar se define lado y nibble de relleno, y si se excluye del conteo. Los ejemplos de laboratorio sólo utilizan fijo y ASCII.

Ejemplo conceptual: contenido BCD de tres dígitos `123`, unidad nibbles y relleno derecho `F` produce `12 3F`; con prefijo ASCII de dos bytes, la longitud 3 produce `30 33`. Con prefijo BCD de un byte produce `03`. Son variantes distintas.

Los importes conservarán sus dígitos. Signo, moneda y exponente decimal serán reglas independientes. No usar `double` para importes ni inferir moneda del idioma del perfil. Track y PIN se tratarán como contenido sensible.

## Bitmaps

Un bitmap primario representa 64 bits: ocho bytes binarios o dieciséis bytes ASCII hex. El secundario agrega otros 64 bits. El bit 1 del primario señala la presencia del secundario; el usuario no lo configura como un campo de negocio. El encoder lo genera y el decoder verifica su consistencia.

En v1 se soporta hasta 128 bits. La política del secundario será `when-needed` o `always`; la segunda puede emitirlo sin campos superiores. No interpretar automáticamente el bit 65 como un tercer bitmap. Esa función requiere otro contrato y pruebas antes de declararse soportada.

## Sobre y campos compuestos

El sobre acepta un literal opcional y segmentos con nombres, longitudes y encodings. Su contenido no se mezcla con los campos del bitmap. El framing permite prefijos de longitud, con definición de si incluyen cabecera, cuerpo o el propio prefijo, exclusivamente en un adaptador posterior.

Los compuestos podrán tener subcampos fijos, prefijos internos, tokens o TLV. Cada prefijo interno debe definir exactamente qué mide. Un contenedor puede conservar bytes opacos si se conoce su límite externo. No afirmar que un token de red sea BER-TLV EMV sin conocer su gramática.

## Reglas y tolerancia

Las reglas tendrán identificador y severidad. Se admitirán condiciones acotadas `present`, `equals`, `in`, `all`, `any` y `not`, sin ejecución de scripts ni carga arbitraria de clases. Acciones iniciales: requerido, prohibido, charset, igualdad con el campo de la solicitud y valores permitidos. La construcción de respuestas será optativa y usará listas explícitas de campos copiables.

El modo estricto rechazará caracteres, rellenos o longitudes no descritos. La compatibilidad permitirá sólo alternativas declaradas, por ejemplo hex minúsculo o un código adicional reconocido. No probar formatos sucesivos hasta que alguno parezca funcionar; no truncar, inventar bytes ni ignorar campos desconocidos de longitud desconocida.

## Validación al compilar

Rechazar campos duplicados, referencias circulares, versión incorrecta, campos fuera de rango, prefijos incapaces de representar el máximo, reglas que refieren campos inexistentes y límites inválidos. Los cambios heredados reemplazarán la definición completa de un campo; el sobre se reemplaza completo y las reglas se reemplazan por ID. Las eliminaciones serán explícitas.

La herencia se resolverá dentro de un catálogo local autorizado, sin URLs remotas. No usar reflexión desde archivos aportados por usuarios. Si un motor no soporta una característica, fallar al compilar el perfil, nunca degradar silenciosamente su significado.
