package main;

import java.io.*;
import java.net.*;

public class Cliente {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String host;
    private final int port;

    public Cliente(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            socket = new Socket(host, port);
            System.out.println("Conectado al servidor en " + host + ":" + port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Hilo para escuchar mensajes del servidor
            new Thread(new ServerListener()).start();

            // Enviar mensajes al servidor desde la consola
            try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
                String userInput;
                System.out.println("Escribe un mensaje para enviar al servidor ('salir' para desconectar):");
                while ((userInput = consoleInput.readLine()) != null) {
                    if (userInput.equalsIgnoreCase("salir")) {
                        disconnect();
                        break;
                    }
                    sendMessage(userInput);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al conectar al servidor: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
            System.out.println("Desconectado del servidor.");
        } catch (IOException e) {
            System.out.println("Error al desconectar: " + e.getMessage());
        }
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("Servidor: " + serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Conexión con el servidor cerrada.");
            } finally {
                disconnect();
            }
        }
    }

    public static void main(String[] args) {
        String host = "localhost"; // Cambiar si el servidor está en otro host
        int port = 12345;          // El puerto debe coincidir con el del servidor

        Cliente cliente = new Cliente(host, port);
        cliente.start();
    }
}

