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
    private String presetName = null; // si se pasa por args, se usa y no se pedirá por consola

    // Constructor
    /**
     * Precondición: El host y el puerto deben ser válidos y no nulos.
     * Postcondición: Se inicializa el cliente con los valores del host y puerto.
     */
    public Cliente(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Constructor que acepta un nombre predefinido
    public Cliente(String host, int port, String presetName) {
        this.host = host;
        this.port = port;
        this.presetName = presetName;
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
        // Iniciar hilo que escucha mensajes del servidor de forma asíncrona
        new Thread(new ServerListener(), "ServerListener").start();

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

        // Si se pasó un nombre predefinido en args, úsalo
        if (presetName != null && pattern.matcher(presetName).matches()) {
            nombre = presetName;
            nombreCorrecto = true;
            System.out.println("Usando nombre predefinido: " + nombre);
        }

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
        // Leer líneas desde la consola y enviarlas al servidor
        try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while ((userInput = consoleInput.readLine()) != null) {
                if (userInput.trim().isEmpty()) continue;
                if (userInput.equalsIgnoreCase("salir")) {
                    sendMessage("salir");
                    break;
                }
                // Normalizar comandos para cortar/Mus: aceptar 'c' o 'm' (cualquier caso) and send uppercase single-letter protocol
                String trimmed = userInput.trim();
                if (trimmed.equalsIgnoreCase("c")) {
                    sendMessage("C");
                    continue;
                } else if (trimmed.equalsIgnoreCase("m")) {
                    sendMessage("M");
                    continue;
                }

                // Enviar cualquier otra entrada del usuario al servidor
                sendMessage(userInput);
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
            // Log de depuración: mostrar lo que enviamos al servidor
            System.out.println("(enviado) -> " + message);
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
                    // Filtrar mensajes de protocolo (ej. COD 23) para no mostrarlos al usuario final
                    if (serverMessage != null && serverMessage.startsWith("COD ")) {
                        // Si necesitas verlos para depuración, descomenta la siguiente línea
                        // System.out.println("(PROTO) " + serverMessage);
                        continue;
                    }
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
        int port = 12346;          // El puerto debe coincidir con el del servidor

        // Parse simple args: --name NAME or first arg treated as name
        String presetName = null;
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if ("--name".equals(args[i]) && i + 1 < args.length) {
                    presetName = args[i + 1];
                    break;
                } else if (!args[i].startsWith("--") && presetName == null) {
                    // take first non-flag arg as name
                    presetName = args[i];
                    break;
                }
            }
        }

        Cliente cliente = new Cliente(host, port, presetName);
        cliente.start();

    }
}
