package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PlayerHandler implements Runnable {
    private final Socket socket;
    private final Servidor servidor;
    private PrintWriter out;
    private BufferedReader in;
    private Jugador jugador;
    private CyclicBarrier cyclicBarrier;

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


            //esperar
           cyclicBarrier.await();

        } catch (IOException e) {
            System.out.println("Se perdió la conexión con el jugador.");
            e.printStackTrace();
        } catch (InterruptedException e) {
           e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } finally {
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

            String linea = in.readLine(); // Intentar leer la línea

            if (linea == null || linea.isEmpty()) {
                System.out.println("Se recibió una línea vacía o nula.");
                return null;
            }

            return linea;
        } catch (IOException e) {
            System.err.println("Error al leer mensaje del jugador: " + e.getMessage());
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

