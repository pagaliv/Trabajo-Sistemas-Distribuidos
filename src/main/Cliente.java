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
    private Scanner scanner;

    public Cliente(String host, int port) {
        this.host = host;
        this.port = port;
    }
    public void start() {
        if (!conectar()) {
            return; // Salir si la conexión falla
        }

        identificarJugador(); // Fase 1: Identificación
        esperarConfiguracionInicial(); // Fase 2: Configuración inicial
        jugar(); // Fase 3: Interacción en el juego
       // disconnect(); // Fase 4: Desconexión
    }
    public boolean conectar() {
        try {
            socket = new Socket(host, port);
            scanner = new Scanner(System.in);
            System.out.println("Conectado al servidor en " + host + ":" + port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (IOException e) {
            System.out.println("Error al conectar al servidor: " + e.getMessage());
            return false;
        }
    }
    public void identificarJugador() {
        String regex = "^[a-zA-Z0-9]+$";
        Pattern pattern = Pattern.compile(regex);
        String nombre = null;
        boolean nombreCorrecto = false;

        while (!nombreCorrecto) {
            System.out.println("Escribe tu nombre de jugador:");
            nombre = scanner.nextLine();
            if (pattern.matcher(nombre).matches()) {
                nombreCorrecto = true;
            } else {
                System.out.println("Nombre inválido. Usa solo letras y números.");
            }
        }
        sendMessage(nombre);
        System.out.println("Nombre enviado al servidor.");
    }
    public void esperarConfiguracionInicial() {
        System.out.println("Esperando configuración inicial del juego...");
        try {
            // Leer mensajes iniciales (como "Tus cartas son..." o "Turno inicial...")
            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                System.out.println("Servidor: " + mensaje);

                // Si el servidor indica que la configuración está lista, salimos del bucle
                if (mensaje.contains("Configuración lista")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al recibir la configuración inicial: " + e.getMessage());
        }
    }
    public void jugar() {
        System.out.println("¡El juego ha comenzado!");
        //Leer jugador y si le toca jugar
        leerVariasLineas();
        System.out.println("Punto de control");
        try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while ((userInput = consoleInput.readLine()) != null) {

                if (userInput.equalsIgnoreCase("salir")) {
                    sendMessage("salir");
                    break;
                }else if(userInput.equalsIgnoreCase("Mus")){
                    sendMessage("Mus");
                    String confirmacion= in.readLine();
                    if(confirmacion.equalsIgnoreCase("OK")){
                        System.out.println("Servidor: OK");
                    }else if(confirmacion.equalsIgnoreCase("ERROR")){
                        System.out.println("Mensaje erroneo");
                    }
                    leerVariasLineas();
                    String apuesta=consoleInput.readLine();
                    if(apuesta.equalsIgnoreCase("Pasar")) {
                        sendMessage("Pasar");
                        String confirmacion = in.readLine();
                        if (confirmacion.equalsIgnoreCase("OK")) {
                            System.out.println("Servidor: OK");
                        } else if (confirmacion.equalsIgnoreCase("ERROR")) {
                            System.out.println("Mensaje erroneo");
                        }
                    }


                }else if(userInput.equalsIgnoreCase("Cortar")){
                    sendMessage("Cortar");
                    String confirmacion= in.readLine();
                    if(confirmacion.equalsIgnoreCase("OK")){
                        System.out.println("Servidor: OK");
                    }else if(confirmacion.equalsIgnoreCase("ERROR")){
                        System.out.println("Mensaje erroneo");
                    }
                }


            }
        } catch (IOException e) {
            System.out.println("Error durante el juego: " + e.getMessage());
        }
    }





    public void disconnect() {

            if (in != null) {
                try{
                    in.close();
                } catch (IOException e) {
                   e.printStackTrace();
                }
            }
            if (out != null) {
                    //No se mete en un try/catch porque el propio out.close tiene dentro del metodo un try/catch
                    out.close();

            }
            if (socket != null && !socket.isClosed()) {
                try{
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            System.out.println("Desconectado del servidor.");

    }


    public void start2() {
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
    public void leerUnaSolaLinea(){
        try{
            if(in !=null){
                String mensaje = in.readLine();
                System.out.println("Servidor: " + mensaje);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void leerVariasLineas(){
        String mensaje;
        try{
            while ((mensaje = in.readLine()) != null) {
                // Si el servidor indica que la configuración está lista, salimos del bucle
                if (mensaje.contains("COD 23")) {
                    break;
                }
                System.out.println("Servidor: " + mensaje);


            }
        } catch (IOException e) {
            e.printStackTrace();
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

