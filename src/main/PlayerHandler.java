package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PlayerHandler implements Runnable {
    private final Socket socket;
    private final Servidor servidor;
    private PrintWriter out;
    private BufferedReader in;
    private Jugador jugador;
    private CyclicBarrier cyclicBarrier;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public PlayerHandler(Socket socket, Servidor servidor, CyclicBarrier c) {
        this.socket = socket;
        this.servidor = servidor;
        this.cyclicBarrier=c;
    }
    public Jugador Jugador(){
        return this.jugador;
    }

    @Override
    public void run() {
        try {
            // Inicialización de flujos
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Mensaje de bienvenida
            out.println("¡Bienvenido al servidor de Mus!");

            // Registrar el jugador
            String nombre = in.readLine();
            if (nombre == null || nombre.trim().isEmpty()) {
                out.println("Nombre inválido. Conexión cerrada.");
                cerrarConexion();
                return; // Terminar el proceso si no se recibe un nombre válido
            }


            jugador = new Jugador(nombre);  // Crear el jugador con su nombre

            // Asignamos el PrintWriter al jugador para enviarle mensajes
            jugador.setOut(out);  // Usamos un método setter en Jugador

            // Añadir jugador al servidor
            servidor.addPlayer(this);
            System.out.println("Jugador conectado: " + jugador.getNombre());// Inicialización de flujos

            // Iniciar hilo lector que registra y encola todas las líneas que lleguen desde el cliente.
            Thread readerThread = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        // Log persistente para depuración: siempre mostramos lo que llega
                        System.out.println("[LOG_RECV] de " + (jugador != null ? jugador.getNombre() : "<unknown>") + ": '" + linea + "'");
                        // Encolamos la línea (put para asegurar encolado)
                        try {
                            messageQueue.put(linea);
                            System.out.println("[LOG_ENQUEUE] de " + (jugador != null ? jugador.getNombre() : "<unknown>") + " queueSize=" + messageQueue.size());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            System.err.println("Enqueue interrumpido para " + (jugador != null ? jugador.getNombre() : "<unknown>"));
                            break;
                        }
                        // Enviar un ACK de depuración al cliente para confirmar recepción
                        try {
                            out.println("[ACK_RECV] " + linea);
                        } catch (Exception e) {
                            System.err.println("No se pudo enviar ACK al cliente: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    // si ocurre un error de lectura, lo mostramos y salimos
                    System.err.println("Error en lector de cliente: " + e.getMessage());
                }
            }, "Reader-" + nombre);
            readerThread.setDaemon(true);
            readerThread.start();


            //esperar a que todos los jugadores lleguen
           cyclicBarrier.await();

            // Mantener el hilo vivo mientras la conexión siga abierta.
            // El resto de la comunicación se hace desde la instancia (Juego) llamando
            // a sendMensajeJugador() / recibirLineaJugador(). Si cerramos aquí
            // el socket, el juego ya no podrá comunicarse.
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Se perdió la conexión con el jugador.");
            e.printStackTrace();
        } catch (InterruptedException e) {
           Thread.currentThread().interrupt();
           e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } finally {
            // Cuando el hilo termina, cerramos recursos si aún están abiertos y removemos al jugador
            cerrarConexion();
            servidor.removePlayer(jugador);
        }
    }


    public void sendMensajeJugador(String msg) {
        if(out!=null){
            out.println(msg );
            System.out.println("Se envio a " + this.jugador.getNombre() + ": "+ msg);
        }else {
            System.out.println("No se pudo enviar a  " + this.jugador.getNombre() + "El mensaje");
        }
    }

    public String recibirLineaJugador() {
        try {
        // Depuración: mostrar estado del hilo y cola antes de bloquear
        System.out.println("[recibirLineaJugador] hilo=" + Thread.currentThread().getName() +
            " interrupted=" + Thread.currentThread().isInterrupted() +
            " queueSize=" + messageQueue.size());

        // Intentar obtener la línea desde la cola (llenada por el readerThread)
        // Usamos take() para bloquear hasta que haya una línea disponible.
        String linea = messageQueue.take();

            if (linea.isEmpty()) {
                System.out.println("Se recibió una línea vacía de " + (jugador != null ? jugador.getNombre() : "<unknown>") + ". Ignorando.");
                return ""; // devolver cadena vacía para que el llamador pueda distinguir
            }

            // Log de depuración: mostrar lo que recibió el servidor de este jugador
            System.out.println("Recibido de " + (jugador != null ? jugador.getNombre() : "<unknown>") + ": '" + linea + "' (thread=" + Thread.currentThread().getName() + ")");

            return linea;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Lectura interrumpida: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void desconectarJugador() {
        try {
            // Informar al jugador de la desconexión
            if (out != null) {
                out.println("Has sido desconectado del servidor.");
            }

            // Remover al jugador del servidor
            servidor.removePlayer(jugador);

            // Cerrar la conexión
            cerrarConexion();

            System.out.println("Jugador desconectado: " + (jugador != null ? jugador.getNombre() : "Desconocido"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void cerrarConexion() {
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }try {
            if (out != null) out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

