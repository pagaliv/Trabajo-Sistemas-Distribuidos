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
    private final List<Jugador> jugadores = Collections.synchronizedList(new ArrayList<>());


    private static final int MAX_CLIENTS = 10; // Número máximo de clientes concurrentes
    private static boolean running = true;

    public static void main(String[] args) {
        // Usar try-with-resources para gestionar ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);

            // Usar un pool de hilos para manejar múltiples clientes
            ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

            while (running) {
                try {
                    // Aceptar nueva conexión
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente conectado desde " + clientSocket.getInetAddress());

                    // Procesar solicitud en un nuevo hilo
                    threadPool.submit(() -> procesarSolicitud(clientSocket));
                } catch (IOException e) {
                    System.out.println("Error al aceptar conexión: " + e.getMessage());
                }
            }

            // Apagar el pool de hilos al cerrar el servidor
            threadPool.shutdown();

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    public static void procesarSolicitud(Socket clientSocket) {
        try (clientSocket) { // Cierra automáticamente el socket al finalizar
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                System.out.println("Mensaje recibido: " + mensaje);

                // Responder al cliente
                out.println("Servidor: Recibí tu mensaje -> " + mensaje);

                if ("salir".equalsIgnoreCase(mensaje)) {
                    System.out.println("Cliente desconectado.");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al procesar solicitud: " + e.getMessage());
        }
    }


    /**
     * Añade un jugador al servidor.
     * Precondición:
     * - El jugador no debe ser nulo.
     * Poscondición:
     * - El jugador será añadido a la lista de jugadores conectados.
     */
    public void addPlayer(Jugador jugador) {
        synchronized (jugadores) {
            if (!jugadores.contains(jugador)) {
                jugadores.add(jugador);
                System.out.println("Jugador añadido: " + jugador.getNombre());
            }
        }
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
            for (Jugador jugador : jugadores) {
                jugador.sendMessage(mensaje);
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




    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

