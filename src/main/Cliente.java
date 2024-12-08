package main;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Cliente {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String host;
    private final int port;
    private Scanner scanner;
    private static boolean ordago = false;

    // Constructor
    /**
     * Precondición: El host y el puerto deben ser válidos y no nulos.
     * Postcondición: Se inicializa el cliente con los valores del host y puerto.
     */
    public Cliente(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Método principal que inicia el flujo del cliente
    /**
     * Precondición: Los métodos internos deben estar correctamente implementados.
     * Postcondición: Se realiza la conexión, identificación, juego y desconexión.
     */
    public void start() {
        if (!conectar()) {
            return; // Salir si la conexión falla
        }

        identificarJugador(); // Fase 1: Identificación
        esperarConfiguracionInicial(); // Fase 2: Configuración inicial
        jugar(); // Fase 3: Interacción en el juego
        disconnect(); // Fase 4: Desconexión
    }

    // Establece la conexión con el servidor
    /**
     * Precondición: El host y el puerto deben ser válidos.
     * Postcondición: Establece la conexión con el servidor y crea los flujos de entrada/salida.
     * @return true si la conexión fue exitosa, false en caso contrario.
     */
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

    // Identifica al jugador solicitando un nombre válido
    /**
     * Precondición: El cliente debe estar conectado al servidor y `out` no debe ser null.
     * Postcondición: Se envía un nombre válido al servidor.
     */
    public void identificarJugador() {
        String regex = "^[a-zA-Z0-9]+$"; // Solo letras y números
        Pattern pattern = Pattern.compile(regex);
        String nombre="";
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
        sendMessage(nombre); // Enviar nombre al servidor
        System.out.println("Nombre enviado al servidor.");
    }

    // Espera la configuración inicial del juego desde el servidor
    /**
     * Precondición: El cliente debe estar conectado al servidor y `in` no debe ser null.
     * Postcondición: Procesa la configuración inicial enviada por el servidor.
     */
    public void esperarConfiguracionInicial() {
        System.out.println("Esperando configuración inicial del juego...");
        try {
            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                System.out.println("Servidor: " + mensaje);

                // Si el servidor indica que la configuración está lista, salir del bucle
                if (mensaje.contains("Configuración lista")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al recibir la configuración inicial: " + e.getMessage());
        }
    }

    // Realiza la lógica principal del juego
    /**
     * Precondición: El cliente debe estar conectado al servidor y los flujos (`in`, `out`) deben estar inicializados.
     * Postcondición: Permite la interacción del cliente con el servidor durante el juego.
     */
    public void jugar() {
        System.out.println("¡El juego ha comenzado!");
        leerVariasLineas();
        try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while ((userInput = consoleInput.readLine()) != null || ordago) {
                if (ordago) {
                    ordago = false;
                    leerVariasLineas();
                } else if (userInput.equalsIgnoreCase("salir")) {
                    sendMessage("salir");
                    break;
                }
                // (El resto de la lógica del juego permanece igual)
            }
        } catch (IOException e) {
            System.out.println("Error durante el juego: " + e.getMessage());
        }
    }

    // Cierra las conexiones y recursos abiertos
    /**
     * Precondición: Ninguna, puede ser invocado en cualquier estado.
     * Postcondición: Se cierran los recursos abiertos como el socket, flujos de entrada/salida.
     */
    public void disconnect() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            out.close(); // out.close ya maneja internamente excepciones
        }
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Desconectado del servidor.");
    }

    // Envía un mensaje al servidor
    /**
     * Precondición: `out` no debe ser null.
     * Postcondición: El mensaje se envía al servidor.
     * @param message Mensaje a enviar.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        } else {
            System.err.println("El flujo de salida es null. No se puede enviar el mensaje.");
        }
    }

    // Lee una sola línea del servidor
    /**
     * Precondición: `in` no debe ser null.
     * Postcondición: Se imprime una línea recibida del servidor.
     */
    public void leerUnaSolaLinea() {
        try {
            if (in != null) {
                String mensaje = in.readLine();
                System.out.println("Servidor: " + mensaje);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lee múltiples líneas del servidor hasta encontrar "COD 23"
    /**
     * Precondición: `in` no debe ser null.
     * Postcondición: Se imprimen múltiples líneas recibidas del servidor.
     */
    public void leerVariasLineas() {
        String mensaje;
        try {
            while ((mensaje = in.readLine()) != null) {
                if (mensaje.contains("COD 23")) { // Condición de terminación
                    break;
                }
                System.out.println("Servidor: " + mensaje);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna para escuchar mensajes del servidor
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

    // Punto de entrada del programa
    public static void main(String[] args) {
        String host = "localhost"; // Cambiar si el servidor está en otro host
        int port = 12345;          // El puerto debe coincidir con el del servidor

        Cliente cliente = new Cliente(host, port);
        cliente.start();

    }
}
