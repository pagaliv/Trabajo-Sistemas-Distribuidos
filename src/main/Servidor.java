package main;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {

    private ServerSocket serverSocket;

    // Allow overriding the port via system property 'server.port' or env var 'SERVER_PORT'
    private static final int PORT;
    static {
        int port = 12346;
        try {
            String prop = System.getProperty("server.port");
            if (prop != null && !prop.isEmpty()) port = Integer.parseInt(prop);
            else {
                String env = System.getenv("SERVER_PORT");
                if (env != null && !env.isEmpty()) port = Integer.parseInt(env);
            }
        } catch (Exception e) {
            // fallback to default
            port = 12346;
        }
        PORT = port;
    }
    private final List<PlayerHandler> jugadores = Collections.synchronizedList(new ArrayList<>());
    // Pool de hilos para manejar clientes (instancia, no estático)
    private ExecutorService threadPool = null;
    private Juego juego = null;
    private CyclicBarrier cyclicBarrier;
    private static final int MAX_CLIENTS = 4; // Número máximo de clientes concurrentes
    public static void main(String[] args) {
        // Crear una instancia del servidor y ejecutarla
        Servidor servidor = new Servidor();
        servidor.start();
    }
    public Servidor(){
        cyclicBarrier= new CyclicBarrier(5);

    }
    public void start() {
        // Inicializar threadPool antes de intentar abrir el socket para poder cerrarlo en finally
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        // Usar try-with-resources para gestionar ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { //Crea el socket del servidor
            // Guardamos el serverSocket en el campo de instancia por si necesitamos cerrarlo luego
            this.serverSocket = serverSocket;
            System.out.println("Servidor iniciado en el puerto " + PORT); //Dice el puerto en el que se crea el server

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Aceptar nueva conexión
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente conectado desde " + clientSocket.getInetAddress());

                    // Procesar solicitud en un nuevo hilo
                    PlayerHandler handler = new PlayerHandler(clientSocket,this,cyclicBarrier);
                    threadPool.submit(handler);

                    //

                } catch (IOException e) {
                    System.out.println("Error al aceptar conexión: " + e.getMessage());
                    // Si el serverSocket está cerrado, salimos del bucle
                    if (serverSocket.isClosed()) break;
                }

            }

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        } finally {
            // Apagar el pool de hilos al cerrar el servidor (si fue inicializado)
            if (threadPool != null) {
                threadPool.shutdown();
            }
            stopServer();
        }
    }




    /**
     * Añade un jugador al servidor.
     * Precondición:
     * - El jugador no debe ser nulo.
     * Poscondición:
     * - El jugador será añadido a la lista de jugadores conectados.
     */
    public void addPlayer(PlayerHandler jugador) {
        synchronized (jugadores) {
            if (contieneJugador(jugador.Jugador())) {
                // Ya existe un jugador con ese nombre, desconectamos la nueva conexión
                jugador.desconectarJugador();
                return;
            }

            jugadores.add(jugador);
            System.out.println("Jugador añadido: " + jugador.Jugador().getNombre());

            if (jugadores.size() == MAX_CLIENTS) {
                startGame();
            }
        }
    }
    private void startGame() {
        // Pre: La lista de jugadores tiene exactamente MAX_CLIENTS jugadores.
        // Pos: Inicia la partida con los jugadores conectados.
        System.out.println("¡Todos los jugadores están conectados! Iniciando la partida...");

        //  enviar un mensaje a todos los jugadores
        broadcastMessage("La partida está comenzando...");

        // Aquí podrías inicializar la lógica del juego, como asignar cartas, iniciar turnos, etc.
        // Esto podría incluir instanciar un objeto de la clase `Juego` y pasarle la lista de jugadores.
        juego = new Juego(jugadores,cyclicBarrier);
        juego.jugar();
    }
    public boolean contieneJugador(Jugador jugador){
        for(PlayerHandler jugadoresIterados: jugadores){
            if(jugadoresIterados.Jugador().equals(jugador)){
                return true;
            }
        }
        return false;
    }
    /**
     * Envía un mensaje a todos los jugadores conectados.
     * Precondición:
     * - La lista de jugadores puede estar vacía.
     * - El mensaje no debe ser nulo.
     * Poscondición:
     * - Todos los jugadores conectados reciben el mensaje.
     */
    public void broadcastMessage(String mensaje) {
        synchronized (jugadores) {
            for (PlayerHandler jugador : jugadores) {
                jugador.Jugador().sendMessage(mensaje);
            }
        }
    }
    /**
     * Elimina un jugador del servidor.
     * Precondición:
     * - El jugador no debe ser nulo.
     * Poscondición:
     * - El jugador es eliminado de la lista de jugadores conectados.
     */
    public void removePlayer(Jugador jugador) {
        synchronized (jugadores) {
            PlayerHandler encontrado = null;
            for (PlayerHandler ph : jugadores) {
                Jugador j = ph.Jugador();
                if (j != null && j.equals(jugador)) {
                    encontrado = ph;
                    break;
                }
            }
            if (encontrado != null) {
                jugadores.remove(encontrado);
                System.out.println("Jugador eliminado: " + jugador.getNombre());
            }
        }
    }
    public void enviarMensajePrivado(Jugador jugador, String mensaje) {
        if (jugador != null && mensaje != null && !mensaje.isEmpty()) {
            jugador.sendMessage(mensaje);  // Usamos el método sendMessage del jugador
            System.out.println("Mensaje privado enviado a " + jugador.getNombre() + ": " + mensaje);
        } else {
            System.out.println("No se puede enviar el mensaje privado. Parámetros inválidos.");
        }
    }



    public void stopServer() {
        broadcastMessage("El servidor se está cerrando...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

