package main;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import MD.Card;

public class Jugador {
    private String nombre;
    private List<Card> mano; // Cartas que tiene el jugador
    private int puntuacion;  // Puntos acumulados en el juego
    private boolean enTurno; // Indica si es el turno del jugador
    private boolean activo;  // Indica si el jugador sigue en la partida
    private PrintWriter out; // Para enviar mensajes al jugador


    // Constructor
    public Jugador(String nombre) {
        this.nombre = nombre;
        this.mano = new ArrayList<>();
        this.puntuacion = 0;
        this.enTurno = false;
        this.activo = true;
        this.out = null; // Inicializado como null, se asignará más tarde
    }


    // Métodos para gestionar las cartas del jugador
    public void recibirCarta(Card carta) {
        if (carta != null) {
            mano.add(carta);
        }
    }
    public void addCard(Card card) {
        // Pre: card no es null
        // Pos: Agrega la carta a la mano del jugador
        if (card != null) {
            this.mano.add(card);
            System.out.println(nombre + " ha recibido la carta: " + card);
        } else {
            System.out.println("La carta proporcionada es nula y no se puede añadir.");
        }
    }
    public void showHand() {
        System.out.println("Mano del jugador " + this.nombre + ":");
        for (Card carta : this.mano) {
            System.out.println(carta.toString());
        }
    }

    public void descartarCartas() {
        mano.clear();
    }

    public List<Card> getMano() {
        return new ArrayList<>(mano);
    }

    // Métodos para gestionar puntos
    public void sumarPuntos(int puntos) {
        this.puntuacion += puntos;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    // Métodos para gestionar el estado del jugador
    public boolean isEnTurno() {
        return enTurno;
    }

    public void setEnTurno(boolean enTurno) {
        this.enTurno = enTurno;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Mostrar la mano del jugador
    public void mostrarMano() {
        System.out.println("Cartas de " + nombre + ":");
        for (Card carta : mano) {
            System.out.println(carta.toString());
        }
    }
    // Método para establecer el PrintWriter
    public void setOut(PrintWriter out) {
        this.out = out;
    }
    @Override
    public String toString() {
        return "Jugador{" +
                "nombre='" + nombre + '\'' +
                ", puntuacion=" + puntuacion +
                ", enTurno=" + enTurno +
                ", activo=" + activo +
                '}';
    }
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    public boolean equals(Jugador other){
        return this.nombre.equals(other.nombre);
    }

}

