package main;

import main.PlayerHandler;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {

    private ServerSocket serverSocket;

    private static final int PORT = 12345;
    private final List<PlayerHandler> jugadores = Collections.synchronizedList(new ArrayList<>());
    static ExecutorService threadPool=null; //lo he hecho estatico porque me lo pedia el compilador, pero no estoy seguro de que tenga que serlo (BORRAR ANTES DE ENTREGAR, SI ERES JAVIER Y LEER ESTO IGNORALO)

    private static final int MAX_CLIENTS = 4; // Número máximo de clientes concurrentes
    public static void main(String[] args) {
        // Crear una instancia del servidor y ejecutarla
        Servidor servidor = new Servidor();
        servidor.start();
    }
    public void start() {
        // Usar try-with-resources para gestionar ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { //Crea el socket del servidor
            System.out.println("Servidor iniciado en el puerto " + PORT); //Dice el puerto en el que se crea el server

            // Usar un pool de hilos para manejar múltiples clientes
            threadPool = Executors.newFixedThreadPool(MAX_CLIENTS); //Crea una pool de hilos que usara a posteriori

            while (!Thread.interrupted()) {
                try {
                    // Aceptar nueva conexión
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente conectado desde " + clientSocket.getInetAddress());

                    // Procesar solicitud en un nuevo hilo
                    PlayerHandler handler = new PlayerHandler(clientSocket,this);
                    threadPool.submit(handler);

                    //

                } catch (IOException e) {
                    System.out.println("Error al aceptar conexión: " + e.getMessage());
                }

            }



        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }finally {
            // Apagar el pool de hilos al cerrar el servidor
            threadPool.shutdown();
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
            if(contieneJugador(jugador.Jugador())){
                jugador.desconectarJugador();
            } else if(!contieneJugador(jugador.Jugador())) {
                jugadores.add(jugador);
                System.out.println("Jugador añadido: " + jugador.Jugador().getNombre());
            } if (jugadores.size() == MAX_CLIENTS) {
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
        Juego juego = new Juego(jugadores);
        juego.iniciar();
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
            if (jugadores.remove(jugador)) {
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

