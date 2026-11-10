# Integración Spring y primer núcleo

Fecha: 9 de octubre de 2026. Implementación inicial: Java 21 con codec propio y adaptador Spring Framework/Spring Integration. Spring Boot facilita ejecutar la demostración. Documentación, ejemplos, nombres propios y diagnósticos en español para Latinoamérica; identificadores Java sin tildes.

## Modelo y formato

`MensajeIso` contiene MTI, campos y una cabecera opaca opcional. `ValorTexto` y `ValorBinario` separan texto de bytes. Los mensajes construidos son instantáneas inmutables; `copiar()` permite preparar otra versión. Los constructores son mutables y no deben compartirse entre hilos.

`PerfilMensaje` define campos, formato del bitmap, obligatoriedad por MTI, prefijo constante, longitud de cabecera y límite total. Cada codec utiliza un perfil inmutable. `reemplazarCampo()` hace explícita una variación; `campo()` rechaza duplicados. No se selecciona el perfil ni se toleran ambigüedades automáticamente.

`CodecIsoBasico` implementa `codificar(MensajeIso)` y `decodificar(byte[])` y puede compartirse entre hilos. El cursor y la salida pertenecen a cada llamada. El sobre tiene este orden: `prefijo constante | cabecera | MTI | bitmap | campos`. El prefijo actual no calcula longitudes de transporte TCP.

El subconjunto admite MTI ASCII, bitmap de 64/128 bits binario o hexadecimal ASCII en mayúsculas, y campos numéricos ASCII, ASCII imprimibles o binarios. FIJA exige exactamente la longitud declarada; LLVAR y LLLVAR usan prefijos de 2 y 3 dígitos ASCII. Las longitudes incluyen únicamente el contenido y se miden en bytes; los campos variables pueden estar vacíos. No hay relleno implícito ni conversión de tipos.

## Activar la integración

Tras `./mvnw install`, agrega `org.gca:trama8583-spring-integracion:1.0-SNAPSHOT` a tu aplicación. Todavía no se publica en Maven Central. Importa la configuración y proporciona un único perfil:

```java
import org.gca.trama8583.nucleo.*;
import org.gca.trama8583.spring.ConfiguracionIntegracionIso;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@Import(ConfiguracionIntegracionIso.class)
class ConfiguracionMensajeria {
    @Bean
    PerfilMensaje perfil() {
        return PerfilMensaje.crear("laboratorio-v1")
                .campo(DefinicionCampo.fijo(11, TipoDato.NUMERICO_ASCII, 6))
                .obligatorios("0800", 11)
                .construir();
    }
}
```

Inyecta `PasarelaIso` mediante el constructor de tu servicio. `pasarela.construir(mensaje)` devuelve bytes y `pasarela.interpretar(trama)` devuelve el modelo. Son operaciones locales de solicitud/respuesta.

| Bean o canal | Entrada | Salida |
| --- | --- | --- |
| `pasarelaIso` | Parámetros tipados de los métodos | Resultado del método |
| `trama8583.decodificacion` | Payload `byte[]` | Payload `MensajeIso` |
| `trama8583.codificacion` | Payload `MensajeIso` | Payload `byte[]` |
| `codecIso` | API del núcleo | API del núcleo |

Los `DirectChannel` ejecutan el codec en el hilo del llamador, sin cola ni persistencia. La pasarela crea el canal temporal de respuesta; si usas los canales directamente necesitas `replyChannel` o `MessagingTemplate.sendAndReceive(...)`. Los flujos conservan cabeceras de aplicación como `folioLocal`, sin correlacionar transacciones remotas. `id` y `timestamp` de Spring no son campos ISO. Véanse los contratos oficiales de [DirectChannel](https://docs.spring.io/spring-integration/reference/channel/implementations.html) y [Messaging Gateway](https://docs.spring.io/spring-integration/reference/gateway.html).

La configuración crea un único codec. Usar múltiples perfiles simultáneos requiere un catálogo o flujos y pasarelas con nombres distintos; ese enrutamiento está pendiente. El tiempo de espera de la pasarela no interrumpe el trabajo síncrono del codec y no equivale a un tiempo límite de una operación remota.

## Errores

`ExcepcionIso` contiene `codigo()`, `campo()` y `posicion()`. Las posiciones empiezan en cero sobre la trama completa, incluido el sobre. `-1` indica que no hay posición de entrada, por ejemplo al validar antes de codificar. Sus mensajes no contienen valores; los `toString()` del modelo y sus valores omiten el contenido.

Spring puede envolver el error en una excepción de mensajería y mantener el mensaje fallido. Con estos flujos síncronos llega al llamador y no genera una respuesta ISO automática. Recorre las causas para obtener `ExcepcionIso`. Registra código, número de campo y posición; no imprimas payloads ni cabeceras confidenciales. El `errorChannel` predeterminado no sanitiza mensajes ni sustituye este contrato: [manejo de errores de Spring Integration](https://docs.spring.io/spring-integration/reference/error-handling.html).

PIN y MAC declarados como campos binarios son únicamente bytes opacos; el codec no los calcula ni valida criptográficamente. Texto ASCII en la trama y documentación en español son requisitos independientes.

## Herramientas y validación

Java 21 es el objetivo de compilación y el BOM Spring Boot 4.1.1 administra las versiones Spring; esta línea acepta Java 21 según los [requisitos oficiales](https://docs.spring.io/spring-boot/system-requirements.html). El núcleo tiene JUnit únicamente en pruebas, sin dependencias externas de ejecución. El adaptador declara Spring explícitamente. La decisión de núcleo propio se refiere al procesamiento ISO.

`./mvnw verify` comprueba los tres vectores de laboratorio con bytes esperados independientes, límites de bitmap y longitudes, sobre, campos binarios, inmutabilidad, errores, truncamientos y entradas aleatorias reproducibles. Las pruebas Spring abren un contexto real, llaman la pasarela en ambos sentidos, comprueban cabeceras y errores y ejecutan 100 solicitudes concurrentes. GitHub Actions utiliza Temurin 21 y el mismo comando.

La demostración ejecutable construye una solicitud `0200`, la interpreta y construye una respuesta `0210` sintética. Imprime el modelo sin valores y la longitud de respuesta, cierra el contexto y termina. No abre sockets ni autoriza operaciones.

## Pendientes

El cargador JSON/YAML, BCD, EBCDIC, bitmap terciario, subcampos/TLV y reglas entre campos permanecen como diseño. El campo 65 puede definirse como dato opaco; un protocolo donde anuncie un bitmap adicional aún no está soportado. Los JSON existentes no se leen en ejecución. Siguen pendientes transporte, catálogo de perfiles, correlación transaccional y servicios criptográficos.

No se redistribuyen normas adquiridas ni manuales privados. La validación de compatibilidad concreta y el archivo de licencia de una primera publicación permanecen pendientes.
