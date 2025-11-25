Resumen de cambios y cómo ejecutar

He realizado una serie de correcciones para que el proyecto sea compilable y para arreglar errores lógicos críticos en el flujo del servidor/juego.

Archivos modificados
## Mus (Java) — Proyecto

Este repositorio contiene una implementación en Java de un servidor y clientes para jugar una partida de Mus (versión en desarrollo v1.0.1).

Este README describe la estructura del código, cómo ejecutar el servidor y los clientes desde cero, los cambios realizados recientemente y las limitaciones actuales.

## Estructura y clases principales

Carpeta `src/main` (lógica de red y juego):

- `main.Servidor` — Punto de entrada del servidor. Escucha conexiones TCP (por defecto puerto 12346), instancia `PlayerHandler` para cada conexión y arranca la partida cuando hay 4 jugadores.
- `main.Cliente` — Cliente de consola que se conecta al servidor, identifica al jugador (nombre), escucha mensajes del servidor y reenvía las líneas que escribes en la consola. Soporta `--name NAME` para iniciar sin pedir el nombre.
- `main.Jugador` — Modelo del jugador (nombre, mano, puntuación). Contiene un pequeño `main` que delega a `main.Cliente` para permitir ejecutar `java -cp out main.Jugador` como cliente.
- `main.PlayerHandler` — Handler por conexión: mantiene los flujos (in/out), lee líneas del cliente y expone `sendMensajeJugador(...)` y `recibirLineaJugador()` para que `Juego` interactúe con el jugador.
- `main.Juego` — Lógica de la partida (reparto, turnos, mus, apuestas, comparación de manos). Actualmente está en desarrollo: implementa ciclos de turno, reparto básico y una versión inicial del flujo de apuestas.

Carpeta `src/MD` (modelo de cartas / utilidades):
Se uso como referencia para hacer la baraja, la clase creada en programación IV. 

- `MD.Card` — Representa una carta (palo, número y representación de cadena).
- `MD.Deck` — Mazo: construcción de la baraja española, extracción y eliminación de cartas.
- `MD.Compara` — Utilidades para comparar manos (parcial/experimental).
- `MD.Suit` — Enumeración de palos.

Carpeta `src/INFO`:

- `CODS` — Códigos de protocolo usados en mensajes (por ejemplo `COD 23` = FIN DE COMUNICACIÓN). Mantener estos códigos ayuda a delimitar bloques de mensajes.

Scripts auxiliares (en la raíz):

- `run_all.sh` — Compila el proyecto y arranca el servidor y 4 clientes; intenta abrir cada cliente en su propio emulador de terminal cuando sea posible, o los lanza en background y guarda logs en `logs/`.
- `stop_all.sh` — Detiene procesos Java que correspondan a `main.Servidor` o `main.Cliente` y deja los logs en `logs/`.

## Cambios recientes (resumen de lo que se ha modificado)

### v1.0.2 (EN DESARROLLO - NO FUNCIONAL)
**Estado**: Esta versión tiene problemas críticos de comunicación cliente-servidor que impiden el funcionamiento del juego.

**Problema conocido**: El servidor no está recibiendo correctamente las respuestas de los clientes durante la fase de Mus/Cortar. Cuando un jugador escribe 'M' o 'C' para indicar Mus o Cortar, el mensaje se envía desde el cliente pero no llega a la lógica del juego en el servidor.

**Cambios implementados**:
- **Cliente (`src/main/Cliente.java`)**:
  - Normalización de comandos Mus/Cortar: ahora acepta 'M' (mayúscula o minúscula) para Mus y 'C' para Cortar, enviando siempre la versión en mayúscula al servidor.
  - Añadido log de depuración `(enviado) -> <mensaje>` para rastrear todo lo que el cliente envía al servidor.

- **Servidor (`src/main/PlayerHandler.java`)**:
  - Implementado un hilo lector dedicado (`readerThread`) que encola todos los mensajes recibidos del cliente en una `BlockingQueue`.
  - `recibirLineaJugador()` ahora lee desde la cola usando `take()` (bloqueante) en lugar de leer directamente del socket.
  - Añadidos logs de depuración: `[LOG_RECV]`, `[LOG_ENQUEUE]`, `[recibirLineaJugador]` y ACK al cliente `[ACK_RECV]`.
  - El hilo principal de `PlayerHandler` se mantiene vivo después del `CyclicBarrier` para no cerrar la conexión prematuramente.

- **Servidor (`src/main/Servidor.java`)**:
  - Soporte para sobrescribir el puerto mediante property `-Dserver.port=XXXX` o variable de entorno `SERVER_PORT`.

- **Juego (`src/main/Juego.java`)**:
  - Actualizado `cortaroMus()` para aceptar tanto las palabras completas ("Mus", "Cortar") como las letras simples ('M', 'C').
  - Añadidos logs `[cortaroMus] Preguntando a...` y `[cortaroMus] respuesta obtenida...` para depuración.
  - Prompt mejorado: "Indica 'M' para Mus o 'C' para Cortar (una sola letra, mayúscula o minúscula aceptada)."

**Síntoma del problema**:
- El cliente muestra `(enviado) -> M` indicando que envió el mensaje.
- El servidor muestra `[recibirLineaJugador] hilo=pool-1-thread-X interrupted=false queueSize=0`, lo que indica que la cola está vacía cuando `Juego` intenta leer la respuesta.
- A veces aparece `[cortaroMus] respuesta obtenida de <jugador>: <null>`, confirmando que no se recibió respuesta.

**Posible causa**: Puede haber un problema de sincronización entre el momento en que el `readerThread` lee del socket y encola el mensaje, y el momento en que `Juego` llama a `recibirLineaJugador()`. Se requiere investigación adicional sobre el orden de ejecución de los hilos y posibles condiciones de carrera.

**Recomendación**: No usar esta versión para jugar. Revertir a v1.0.1 o esperar a la corrección en v1.0.3.

---

### v1.0.1
Nota: estos cambios se hicieron para corregir errores de sincronización, comparación de jugadores y mejorar la experiencia de ejecución local.

- `src/main/Jugador.java`
  - Añadido `equals(Object)` y `hashCode()` correctos.
  - Añadido un `public static void main(String[] args)` que delega a `main.Cliente` para poder ejecutar `java -cp out main.Jugador` como cliente.

- `src/main/Cliente.java`
  - Acepta `--name NAME` (o primer argumento) para usar un nombre predefinido y no pedirlo por consola.
  - Añadido filtrado de líneas de protocolo que empiezan por `COD ` para que los usuarios no vean terminadores internos (p.ej. `COD 23`).
  - Añadido constructor alternativo `Cliente(String host,int port,String presetName)`.

- `src/main/PlayerHandler.java`
  - Mejoras en `recibirLineaJugador()` para loguear las líneas recibidas y distinguir entre nulo / vacío; limpieza de recursos en desconexiones.
  - `sendMensajeJugador(...)` imprime en el log del servidor qué mensaje se envió a quién (útil para depurar).

- `src/main/Servidor.java`
  - Uso de un `ExecutorService` para manejar clientes.
  - `addPlayer` y `removePlayer` revisados para manejar correctamente jugadores con nombres duplicados y eliminar `PlayerHandler` asociado.
  - Puerto por defecto usado en scripts y en el cliente: 12346.

- `src/main/Juego.java`
  - Reparto inicial y notificaciones a jugadores.
  - Notificación de turno ahora se envía privadamente al jugador activo y los otros reciben una notificación de espera.
  - `cortaroMus()` ahora informa con un broadcast cuando un jugador corta (`Mus cortado por <nombre>`) y trata entradas nulas/vacías con mayor robustez.
  - Correcciones de índices modulares para evitar IndexOutOfBounds en recambios.

- Scripts añadidos/actualizados:
  - `run_all.sh` y `stop_all.sh` para compilar, lanzar y detener servidor/clientes (logs en `logs/`).

Si necesitas un diff más detallado de los cambios, puedo generarlo o listar las secciones de cada archivo modificadas.

## Cómo construir y ejecutar desde cero (pasos reproducibles)

1. Asegúrate de tener JDK 11+ instalado.

2. (Opcional) Si usas VS Code, cierra o reinicia la ventana si la extensión Java deja procesos con clases cargadas desde el workspace (esto evita conflictos de puerto).

3. Compilar:

```bash
cd /home/pgalileavalverde/projets/practicar/Trabajo-Sistemas-Distribuidos
mkdir -p out
javac -d out $(find src -name "*.java")
```

4. Ejecutar el servidor (terminal A):

```bash
java -cp out main.Servidor
```

5. Ejecutar clientes (terminales B,C,D,E) — interactivos:

```bash
java -cp out main.Cliente        # pedirá nombre
java -cp out main.Cliente --name Alice
java -cp out main.Jugador --name Bob  # equivalente a Cliente
```

6. Alternativa: usar `run_all.sh` para lanzar servidor + 4 clientes (crea logs en `logs/`):

```bash
./run_all.sh
# para detener lo lanzado por el script:
./stop_all.sh
```

## Protocolo y códigos (archivo `src/INFO/CODS`)

El repositorio incluye `src/INFO/CODS` con los códigos que emplea el servidor para marcar estados de comunicación. Ejemplos:

- `COD 23`: FIN DE COMUNICACIÓN (usado para indicar fin de bloque de mensajes al cliente)
- `COD 28`: La apuesta supera el valor necesario y se considera ordago
- `COD 19`: La apuesta no supera el valor necesario
- `COD 69`: El otro equipo quiere hacer ordago

El cliente actual filtra las líneas que empiezan por `COD ` para no mostrarlas directamente al usuario. Si deseas que se muestren (por ejemplo para depuración), coméntalo en el cliente.

## Limitaciones conocidas y próximos pasos recomendados

- Lógica del Mus incompleta: la resolución de apuestas, comparaciones exactas de manos y manejo de empates requieren mayor trabajo para cumplir todas las reglas del Mus.
- Robustez de entradas: actualmente el servidor asume formatos concretos de respuesta. Recomiendo normalizar y validar (trim, casefold) todas las entradas y añadir timeouts y reintentos.
- Separación de protocolo/visualización: implementar mensajes estructurados (ej. `TYPE:TURN|WHO:pepe`) reducirá ambigüedad entre mensajes de control y mensajes legibles por el jugador.
- Tests: añadir pruebas unitarias para `MD` (mazo, comparación) y tests de integración para el flujo servidor-client.

## Cómo contribuir / pruebas rápidas

- Para ejecutar una sesión rápida en tu máquina: compila con `javac -d out $(find src -name "*.java")`, lanza el servidor y 4 clientes con `./run_all.sh` o manualmente en cuatro terminales.
- Revisa `logs/` para ver `server.log` y `client_*.log` que contienen trazas útiles para depuración.

# Autor

- Desarrollado y mantenido por [pagaliv](https://github.com/pagaliv)(Menuda documentación se ha marcado)
