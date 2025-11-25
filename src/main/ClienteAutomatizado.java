package main;

import java.io.*;
import java.net.Socket;

/**
 * Cliente automatizado para pruebas que simula la interacción de un usuario real
 */
public class ClienteAutomatizado {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nombre;

    public ClienteAutomatizado(String nombre, String host, int port) {
        this.nombre = nombre;
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("[" + nombre + "] Conectado al servidor");
        } catch (IOException e) {
            System.err.println("[" + nombre + "] Error al conectar: " + e.getMessage());
        }
    }

    public void ejecutar() {
        Thread receptor = new Thread(() -> {
            try {
                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    System.out.println("[" + nombre + "] << " + mensaje);
                    
                    // Si nos piden que elijamos Mus/Cortar, respondemos automáticamente
                    if (mensaje.contains("Indica 'M' para Mus")) {
                        // Pequeña pausa para simular que el usuario está leyendo
                        Thread.sleep(500);
                        System.out.println("[" + nombre + "] >> M");
                        out.println("M");
                        out.flush(); // Asegurar que se envíe inmediatamente
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("[" + nombre + "] Error en receptor: " + e.getMessage());
            }
        });
        receptor.setDaemon(true);
        receptor.start();

        try {
            // Enviar nombre
            Thread.sleep(500);
            System.out.println("[" + nombre + "] >> " + nombre);
            out.println(nombre);
            out.flush();

            // Mantener el cliente vivo
            Thread.sleep(30000);
            
        } catch (InterruptedException e) {
            System.err.println("[" + nombre + "] Interrumpido: " + e.getMessage());
        } finally {
            cerrar();
        }
    }

    private void cerrar() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("[" + nombre + "] Desconectado");
        } catch (IOException e) {
            System.err.println("[" + nombre + "] Error al cerrar: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String nombre = args.length > 0 ? args[0] : "AutoBot";
        String host = args.length > 1 ? args[1] : "localhost";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 12346;

        ClienteAutomatizado cliente = new ClienteAutomatizado(nombre, host, port);
        cliente.ejecutar();
    }
}
