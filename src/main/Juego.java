package main;

import MD.*;
import java.util.ArrayList;
import java.util.List;

    public class Juego {
        private List<Jugador> jugadores;
        private Deck deck;
        private int indiceJugadorActual;

        // Constructor
        public Juego(List<Jugador> jugadores) {
            // Pre: La lista de jugadores no está vacía
            // Post: Inicializa el juego con los jugadores y un mazo de cartas
            this.jugadores = new ArrayList<>(jugadores);
            this.deck = new Deck();
            this.deck.fillBarajaEspanola();
            this.indiceJugadorActual = 0;
        }

        // Método para inicializar el juego
        public void iniciarJuego() {
            // Barajar el mazo
            this.deck.shuffle();

            // Repartir cartas a los jugadores
            repartirCartas();

            // Notificar el inicio del juego
            System.out.println("¡El juego ha comenzado!");
            notificarTurnoActual();
        }

        // Reparte cartas a los jugadores
        private void repartirCartas() {
            int cartasPorJugador = 4; // Ejemplo: cada jugador recibe 4 cartas
            for (int i = 0; i < cartasPorJugador; i++) {
                for (Jugador jugador : jugadores) {
                    if (deck.getNumeroCartas() > 0) {
                        jugador.addCard(deck.deleteCardInPosition(0));
                    }
                }
            }
        }

        // Cambia al siguiente turno
        public void pasarTurno() {
            indiceJugadorActual = (indiceJugadorActual + 1) % jugadores.size();
            notificarTurnoActual();
        }

        // Valida si el jugador puede realizar una acción en este turno
        public boolean validarTurno(Jugador jugador) {
            return jugadores.get(indiceJugadorActual).equals(jugador);
        }

        // Notifica de quién es el turno actual
        private void notificarTurnoActual() {
            Jugador jugadorActual = jugadores.get(indiceJugadorActual);
            System.out.println("Es el turno de: " + jugadorActual.getNombre());
        }

        // Maneja la acción realizada por un jugador
        public void manejarAccion(Jugador jugador, String accion) {
            if (!validarTurno(jugador)) {
                System.out.println("No es tu turno, " + jugador.getNombre());
                return;
            }

            // Procesar la acción (aquí puedes agregar lógica según las reglas del juego)
            System.out.println(jugador.getNombre() + " realiza la acción: " + accion);

            // Después de procesar la acción, pasa al siguiente turno
            pasarTurno();
        }

        // Método para finalizar el juego
        public void finalizarJuego() {
            System.out.println("El juego ha terminado.");
            // Puedes agregar lógica para determinar el ganador o limpiar recursos
        }
    }

