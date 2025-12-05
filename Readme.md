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
### v1.1.0 (MINOR - 05 Dec 2025)
**Estado**: MINOR — Nueva persistencia XML (DOM) y mejora del flujo de apuestas

Resumen de cambios (v1.1.0):

- Persistencia XML (DOM): se añadió una clase `src/main/GamePersistence.java` que escribe un snapshot del estado de la partida en `game_snapshot.xml` cada vez que se reparten cartas (`Juego.repartirCartas()` hace la llamada por defecto). El snapshot contiene metadata (timestamp, índice de jugador actual), lista de jugadores (nombre, puntuación, activo) y sus manos (cartas con value/suit). Esta persistencia es una snapshot ligera pensada para inspección y recuperación manual.
- Avance de fase automático al aceptar apuestas: cuando un adversario acepta una apuesta en fases como Grandes o Pequeñas, la partida avanza correctamente a la siguiente fase en lugar de quedarse bloqueada. Se introdujeron flags (`grandesAccepted`, `pequeAccepted`) y un helper `promptAdversariosAceptarApuesta(...)` que notifica a los rivales en orden. Además `procesarFaseApuesta(...)` puede retornar un código especial (-3) para indicar que la apuesta fue aceptada y que hay que avanzar de fase.
- Envío de manos a clientes reforzado: confirmado que los jugadores reciben su mano al inicio de cada mano (ya presente en v1.0.5, integrado y verificado junto con la persistencia).
- Limpieza y estabilidad: se limpió la clase `Juego.java` quitando bloques duplicados y restaurando coherencia tras las modificaciones iterativas; se añadieron getters públicos (`getJugadores()`, `getIndiceJugadorActual()`) para que `GamePersistence` pueda serializar el estado sin romper encapsulamiento.
- Compatibilidad de formatos de apuesta: mantiene la aceptación de variantes ("Apostar <n>", "Apostar" + número en la siguiente línea, o solo el número) y robustez en el parseo.

Detalles técnicos y uso:

- Archivo de snapshot por defecto: `game_snapshot.xml` en el directorio de trabajo del servidor. Contenido mínimo:
  - `<GameSnapshot timestamp="..." currentPlayerIndex="N">`
  - `<Players><Player index="0" name="Alice" score="10" active="true"><Hand><Card value="1" suit="OROS"/></Hand></Player>...</Players>`
  - `</GameSnapshot>`
 - Cambiar comportamiento / ruta: por ahora la ruta está codificada en la llamada dentro de `Juego.repartirCartas()`. Para personalizarla, modifique la llamada en `Juego.repartirCartas()` o extraiga la ruta a una propiedad de configuración.
 - Avance de fase por aceptación: cuando un adversario responde "A" (aceptar) tras una apuesta, la función de control procesa ese evento y avanza la fase; si la respuesta es negativa o no llega en timeout, el flujo normal de pasar/contraapuesta continúa. El timeout por defecto para lecturas sigue siendo el de `PlayerHandler.recibirLineaJugador()` (poll(30s)). Es posible ajustar un timeout específico para prompts de aceptación si se desea una respuesta más rápida.

Archivos añadidos/modificados clave en este lanzamiento:

- `src/main/GamePersistence.java` — nueva: serializa snapshot DOM.
- `src/main/Juego.java` — modificado: llamadas a `GamePersistence.saveSnapshot(...)` tras repartir, getters públicos (`getJugadores()`, `getIndiceJugadorActual()`), flags de fase (`grandesAccepted`, `pequeAccepted`), y lógica para avanzar fase cuando un adversario acepta una apuesta.

Impacto y próximos pasos recomendados:

- Verificar los snapshots: lanza una partida, espera al primer reparto y abre `game_snapshot.xml` para inspeccionar la estructura.
- Hacer la ruta de snapshot configurable y añadir `loadGameFromXml(...)` para restaurar partidas (pendiente/puede implementarse como v1.2.0 si se desea).
 - Ajustar timeout de prompts de aceptación (actual 30s) si se desea una UX más rápida: esto puede implementarse con un timeout específico para prompts de aceptación sin modificar el comportamiento global de `recibirLineaJugador()`.

Compilación y verificación:

- El proyecto fue compilado localmente con `javac` tras las modificaciones y no presenta errores de compilación (se verificó `javac -d out src/MD/*.java src/main/*.java`).

Notas finales:

- Esta versión introduce persistencia no intrusiva y una mejora funcional clave en las apuestas: aceptar hace avanzar fases. Para generar ejemplos de `game_snapshot.xml`, ejecute una partida y abra el archivo generado; para documentar un proceso de restauración es necesario implementar antes `loadGameFromXml(...)`.

### v1.0.5 (PATCH - 05 Dec 2025)
**Estado**: PATCH — Mejoras en reparto y en el flujo de apuestas

Resumen de cambios rápidos (v1.0.5):

 - Envío de la mano al cliente al comienzo de cada mano: `Juego.repartirCartas()` ahora limpia manos previas, reparte cartas y llama a `leerCartasJugador(...)` para que cada cliente vea inmediatamente su mano en la consola.
 - Robustecimiento del reparto: `repartirCartas()` ahora descarta manos anteriores antes de repartir para evitar acumulación de cartas en pruebas continuas.
 - Flujo de apuestas más tolerante: en las fases Grandes/Pequeñas/Pares y en el helper `procesarFaseApuesta(...)` se aceptan formatos adicionales de apuesta:
   - "Apostar" y luego la cantidad en la siguiente línea (compatibilidad hacia atrás).
   - "Apostar <cantidad>" en una sola línea.
   - "<cantidad>" (número solo en una línea).
   Esto reduce respuestas "ERROR" cuando clientes manuales o bots usan variantes razonables del protocolo.
 - Validaciones adicionales: entradas numéricas en apuestas se parsean con control de errores y se validan antes de aplicarlas.
 - Compilación verificada localmente tras los cambios.

Archivos modificados en este parche:

 - `src/main/Juego.java` — limpieza de manos antes de repartir, envío de la mano al cliente, y aceptación de nuevos formatos de apuesta (inline y numérico).

Impacto y próximos pasos:

 - Estos cambios mejoran la experiencia del jugador (ven su mano al inicio de la mano) y reducen errores de protocolo en la fase de apuestas. Siguen sin cambiar las reglas del juego.
 - Recomendado: ejecutar un playtest con 4 clientes para verificar apuestas en los tres formatos ("Apostar\n5", "Apostar 5", "5").

### v1.0.4 (PATCH - 05 Dec 2025)
**Estado**: PATCH — Correcciones de robustez y validaciones adicionales

Resumen de cambios rápidos (v1.0.4):

- Añadida repartición inicial de cartas al inicio de cada mano para asegurar que los jugadores tienen mano antes de la fase Mus/Cortar (`Juego.repartirCartas()` se invoca al comienzo de la mano).
- Reforzada la función de descarte (`repartirCartasMus()`): validación de entrada (null/empty), parseo seguro con manejo de NumberFormatException, validación de índices (evita IndexOutOfBounds) y comportamiento de reintento/informe al jugador en caso de error.
- Evitados NPEs en la resolución de órdago: comprobaciones `null` antes de usar `equalsIgnoreCase` para las respuestas adversarias (resp1/resp2).
- Uso de `jugadores.size()` donde era posible en la fase de recambios para evitar asumir un tamaño fijo (más tolerante si cambias el número de participantes en pruebas).
- Compilación verificada localmente: `javac -d out $(find src -name "*.java")` completó sin errores.

Archivos modificados en este parche:

- `src/main/Juego.java` — Lógica de reparto y validaciones en la fase de intercambio/descartes; protecciones contra entradas inválidas y NPEs.

Impacto y próximos pasos:

- Estos cambios son de bajo riesgo y evitan fallos comunes que causaban cierres abruptos o excepciones durante la fase de descarte y ordago. No cambian reglas del juego, sólo aumentan la robustez del protocolo y la tolerancia a entradas erróneas.
- Recomendado: ejecutar una partida de prueba (servidor + 4 clientes automatizados) para validar el flujo completo (aún quedan tareas pendientes en la lista de prioridades: refactorizar CyclicBarrier y añadir tests de integración automatizados).


### v1.0.3 (FUNCIONAL - 25 Nov 2025)
**Estado**: FUNCIONAL - Problema crítico de comunicación cliente-servidor RESUELTO

**Problemas resueltos**:

1. **Condición de carrera en PlayerHandler**: El servidor no estaba recibiendo las respuestas de los clientes durante la fase de Mus/Cortar porque cuando el cuarto jugador se conectaba, liberaba el CyclicBarrier antes de que su propio readerThread estuviera listo.

2. **Bloqueo en Cliente.esperarConfiguracionInicial()**: El cliente nunca llegaba al método jugar() porque esperarConfiguracionInicial() se quedaba bloqueado indefinidamente compitiendo con ServerListener por leer del mismo BufferedReader.

**Mejoras implementadas**:

**Servidor (PlayerHandler.java)**:
- Reordenada la inicialización: el readerThread ahora se crea e inicia ANTES de llamar a servidor.addPlayer(this)
- Esto garantiza que todos los hilos lectores estén operativos antes de que el CyclicBarrier se libere y comience el juego
- Arquitectura de hilos mejorada:
  - Cada PlayerHandler tiene un hilo lector dedicado que lee continuamente del socket
  - Los mensajes se encolan en una BlockingQueue<String>
  - recibirLineaJugador() usa messageQueue.poll(30, TimeUnit.SECONDS) con timeout
  - El hilo principal se mantiene vivo después del barrier para no cerrar la conexión prematuramente
- Logging simplificado: eliminados logs excesivos de depuración, mantenidos solo los esenciales

**Cliente (Cliente.java)**:
- Eliminada la llamada a esperarConfiguracionInicial() en el método start()
- Ahora el flujo es: conectar() -> identificarJugador() -> jugar() -> disconnect()
- El ServerListener (hilo asíncrono) maneja todos los mensajes del servidor sin bloquear el flujo principal
- Añadido flush() explícito en sendMessage() para garantizar envío inmediato
- Añadidos mensajes de depuración [DEBUG] cuando se envían comandos M o C
- Mejores instrucciones al usuario al inicio del juego

**Juego (Juego.java)**:
- Eliminado logging excesivo en cortaroMus()
- Mantenida la lógica para aceptar tanto 'M'/'C' como 'Mus'/'Cortar' (mayúsculas/minúsculas)

**Pruebas realizadas**:
- Prueba automatizada con 4 clientes Java (ClienteAutomatizado.java): todos los jugadores respondieron correctamente
- Verificado que todos los readerThreads se inician antes del comienzo del juego
- Confirmado que los mensajes M/C se reciben correctamente en el servidor
- Sin timeouts ni desconexiones inesperadas

**Cómo usar**:
```bash
# Terminal 1: Servidor
java -cp out main.Servidor

# Terminales 2-5: Clientes
java -cp out main.Cliente --name Jugador1
java -cp out main.Cliente --name Jugador2
java -cp out main.Cliente --name Jugador3
java -cp out main.Cliente --name Jugador4
```

Cuando veas "Indica 'M' para Mus o 'C' para Cortar", simplemente escribe M o C y presiona Enter.

---

### v1.0.2 (DEPRECADO - NO USAR)
**Estado**: NO FUNCIONAL - contiene condición de carrera y bloqueo en cliente

**Cambios implementados**:
- Implementado hilo lector dedicado (readerThread) en PlayerHandler con BlockingQueue
- recibirLineaJugador() modificado para usar messageQueue.poll() con timeout
- Protocolo simplificado: aceptación de 'M'/'C' para Mus/Cortar
- Añadidos logs de depuración extensivos ([LOG_RECV], [LOG_ENQUEUE], [ACK_RECV])

**Problemas conocidos**: 
- Condición de carrera: readerThread se iniciaba DESPUÉS de addPlayer(), causando que el cuarto jugador nunca recibiera mensajes
- Cliente bloqueado indefinidamente en esperarConfiguracionInicial() compitiendo con ServerListener

**Recomendación**: Actualizar a v1.0.3 o superior.

---

### v1.0.1
**Estado**: Mejoras de sincronización y experiencia de usuario

**Cambios principales**:
- Implementados métodos equals() y hashCode() en Jugador.java para correcta comparación
- Añadido main() en Jugador.java que delega a Cliente para facilitar ejecución
- Cliente acepta --name NAME para uso en scripts automatizados
- Filtrado de mensajes de protocolo (COD XX) en el cliente para mejor experiencia visual
- Mejoras en PlayerHandler: mejor logging y manejo de desconexiones
- Servidor usa ExecutorService para gestión de hilos de clientes
- Correcciones en Juego: índices modulares, broadcast cuando se corta Mus, mejor manejo de entradas nulas
- Scripts run_all.sh y stop_all.sh para automatización

---

### v1.0.0
**Estado**: Versión inicial con errores de compilación y lógica

**Características implementadas**:
- Servidor TCP básico en puerto 12346
- Sistema de turnos y reparto de cartas
- Estructura de clases: Servidor, Cliente, Jugador, PlayerHandler, Juego
- Modelo de cartas: Deck, Card, Suit, Compara
- Códigos de protocolo definidos en INFO/CODS

**Problemas conocidos**:
- Errores de compilación en múltiples archivos
- Falta de sincronización entre hilos
- Comparación incorrecta de jugadores (sin equals/hashCode)
- Manejo deficiente de desconexiones
- Sin scripts de automatización

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

El cliente actual filtra las líneas que empiezan por `COD ` para no mostrarlas directamente al usuario. Para verlas durante la depuración, habilite la impresión de mensajes de protocolo en `src/main/Cliente.java`.

## Limitaciones conocidas y próximos pasos
En el caso de repetir la asignatura una vez más, que espero por todo lo que quiero que asi no sea, las posibles mejoras serían: 
- Lógica del Mus mejorable: la resolución de apuestas, comparaciones exactas de manos y manejo de empates requieren mayor trabajo para cumplir todas las reglas del Mus.
- Robustez de entradas: actualmente el servidor asume formatos concretos de respuesta. Recomiendo normalizar y validar (trim, casefold) todas las entradas y añadir timeouts y reintentos.
- Separación de protocolo/visualización: implementar mensajes estructurados (ej. `TYPE:TURN|WHO:pepe`) reducirá ambigüedad entre mensajes de control y mensajes legibles por el jugador.
- Tests: añadir pruebas unitarias para `MD` (mazo, comparación) y tests de integración para el flujo servidor-client.
- Crear salas a las que poder apuntarse. 

## Cómo contribuir / pruebas rápidas

- Para ejecutar una sesión rápida en tu máquina: compila con `javac -d out $(find src -name "*.java")`, lanza el servidor y 4 clientes con `./run_all.sh` o manualmente en cuatro terminales.
- Revisa `logs/` para ver `server.log` y `client_*.log` que contienen trazas útiles para depuración.

# Autor

- Desarrollado y mantenido por [pagaliv](https://github.com/pagaliv)
