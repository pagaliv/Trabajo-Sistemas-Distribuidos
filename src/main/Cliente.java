package main;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        boolean nombreCorrrecto=false;
        String regex = "^[a-zA-Z0-9]+$";
        String nombre= null;
        Pattern pattern = Pattern.compile(regex); // Compilar la expresión regular


        while (!nombreCorrrecto){
                System.out.println("Escribe tu nombre de jugador");
                Scanner scanner = new Scanner(System.in);
                nombre=scanner.nextLine();
                Matcher matcher = pattern.matcher(nombre);
                nombreCorrrecto= matcher.matches();
            }


        try {
            //inicialización de variables
            socket = new Socket(host, port);
            System.out.println("Conectado al servidor en " + host + ":" + port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            sendMessage(nombre);
            // Hilo para escuchar mensajes del servidor
            new Thread(new ServerListener()).start();

            // Enviar mensajes al servidor desde la consola
            try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
                String userInput;
                System.out.println("Escribe un mensaje para enviar al servidor ('salir' para desconectar):");
                while ((userInput = consoleInput.readLine()) != null) {
                    if (userInput.equalsIgnoreCase("salir")) {

                        break;
                    }
                    sendMessage(userInput);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al conectar al servidor: " + e.getMessage());
        }finally { //cerrar recursos
            disconnect();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        } else {
            System.err.println("El flujo de salida es null. No se puede enviar el mensaje.");
        }
    }


    public void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Desconectado del servidor.");
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos: " + e.getMessage());
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

