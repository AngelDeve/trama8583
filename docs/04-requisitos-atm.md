# Requisitos generalizados para mensajería ATM

## Uso del caso de estudio

La guía de ocho páginas aportada es un resumen de estudio de una interfaz de emisor. No contiene el manual completo, su revisión contractual ni todas las gramáticas. Se utiliza para detectar capacidades necesarias y preguntas de validación. El proyecto público no incluirá el nombre de la contraparte ni presentará un perfil de laboratorio como perfil certificado.

Cambiar un nombre no concede permiso para publicar un manual, sus tablas o un perfil derivado. El perfil real debe mantenerse bajo control del integrador hasta aclarar sus derechos de distribución.

## Capacidades requeridas

| ID | Capacidad del componente | Origen en la guía | Estado |
| --- | --- | --- | --- |
| ATM-01 | Literal opcional antes de cabecera y MTI | Páginas 1 y 3 | Diseño general; bytes reales por confirmar |
| ATM-02 | Cabecera segmentada y versionada | Páginas 3 y 4 | Necesita catálogo de códigos autorizado |
| ATM-03 | Bitmap hexadecimal y bloque secundario configurable | Páginas 3, 4 y 6 | Implementable con vectores sintéticos |
| ATM-04 | Longitud fija de terminal sobrescribible por perfil | Páginas 1 y 4 | Ejemplo sintético de cambio 8 a 16 |
| ATM-05 | Campo de montos configurable como fijo o variable | Páginas 1 y 4 | Sin imponer una forma global |
| ATM-06 | Presencia por MTI, operación y resultado | Página 4 | Condiciones completas pendientes |
| ATM-07 | Campos de cuentas de origen y destino | Páginas 1 y 4 | Prefijos y máximos pendientes |
| ATM-08 | Comparación entre solicitud y respuesta | Páginas 1 y 8 | Regla de eco configurable; contenido sensible |
| ATM-09 | Compuestos con indicadores internos de longitud | Página 5 | Gramática y conteo pendientes |
| ATM-10 | Tokens con validación y bytes opacos | Página 5 | Falta formato, ubicación y longitud |
| ATM-11 | Mensajes de rechazo y códigos de cabecera separados de negocio | Página 3 | Ejemplos inconsistentes; no automatizar |
| ATM-12 | Representación de reversos, red y mensajes financieros | Páginas 2 a 4 | Sin ejecutar tiempos o reintentos en el codec |

Las referencias a páginas permiten revisar la guía local. No acreditan que sus afirmaciones sean normativas.

## Ambigüedades que bloquean un perfil de producción

| Hallazgo | Consecuencia | Confirmación requerida |
| --- | --- | --- |
| MTI de cuatro dígitos junto a un ejemplo de rechazo de cinco | Desplazamiento de bitmap si se codifica literalmente | Transformación exacta, con vector aprobado; no corregir por conjetura |
| Indicador interno de dos posiciones descrito con un valor de tres dígitos | No se sabe dónde comienza el siguiente subcampo | Distinguir prefijo externo, longitud interna y contenido |
| Tipo numérico que también admite signo menos | Un validador de dígitos rechazaría mensajes previstos | Longitud del signo, posición y representación positiva |
| Códigos de estado concatenados y referencias de nota mezcladas | Catálogo ambiguo | Tabla original legible y revisión vigente |
| Campos variables sin máximo ni prefijo completo | No hay límites fiables para decodificar | Encoding, unidad, máximo y gramática por campo |
| Texto especial de reverso con distinta cantidad de asteriscos | No se conoce la representación de 40 posiciones | Bytes exactos y política de espacios |
| Campos de control criptográfico sin cobertura de MAC | Es imposible verificar autenticidad | Algoritmo, bytes cubiertos, encoding y vectores HSM |
| Tokens descritos sólo por finalidad | No se puede decodificar ni construir respuesta EMV | Contenedor, versión, tags, delimitadores, orden y obligatoriedad |

La descripción genérica de LLVAR no permite concluir que todos los campos variables de esta interfaz usen ese formato. Las marcas condicionales necesitan predicados completos; una celda vacía tampoco debe convertirse automáticamente en prohibición sin confirmar el manual.

## Información a pedir al integrador

Solicitar manual autorizado y revisión, tabla completa de formatos y condiciones, envelope y framing, restricciones de charset, reglas de reversos y rechazo, vectores sintéticos válidos e inválidos con bytes esperados y protocolo de certificación. Para compuestos, pedir ejemplos de fronteras y prefijos, no sólo un listado de nombres.

Los límites de respuesta y reenvío descritos en la guía serán parámetros de la aplicación. Debe aclararse desde qué evento se mide cada plazo, qué ocurre con duplicados y respuestas tardías, y cómo se correlacionan mensajes. STAN o RRN aislados no se tratarán como claves universalmente únicas.

## Seguridad del diseño

El codec transportará bloques PIN, MAC y datos criptográficos como bytes o representaciones explícitas. No almacenará claves ni implementará el esquema criptográfico resumido en la guía. La integración HSM definirá por separado operaciones, formatos de bloques y gestión de claves conforme al contrato vigente.

Los mensajes, errores y `toString` deben redactar por defecto contenido sensible. No registrar tramas completas, tracks, PIN, claves ni valores de cuentas. Los campos privados sin clasificación se consideran sensibles. Se usarán únicamente datos sintéticos en ejemplos y pruebas públicas. Los límites de parsing se aplicarán antes de reservar memoria; se prohibirán entidades XML externas si el motor las utiliza para configuración.
