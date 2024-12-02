package main;

import MD.*;
import java.util.ArrayList;
import java.util.List;

    public class Juego {
        private  List<PlayerHandler> jugadores;
        private  Deck deck;
        private int indiceJugadorActual;

        // Constructor
        public Juego(List<PlayerHandler> jugadores) {
            // Pre: La lista de jugadores no está vacía
            // Post: Inicializa el juego con los jugadores y un mazo de cartas
            this.jugadores = new ArrayList<>(jugadores);
            this.deck = new Deck();
            this.deck.fillBarajaEspanola();
            this.indiceJugadorActual = 0;
        }

        // Método para inicializar el juego

        public void iniciar() {
            int ronda;
            System.out.println("La partida ha comenzado.");
            informarCompanyero();
            //  repartir cartas a los jugadores
            deck.shuffle();
            for (PlayerHandler jugador : jugadores) {
                for (int i = 0; i < 4; i++) { // Repartir 4 cartas por jugador
                    jugador.Jugador().addCard(deck.extractCard());
                }
                jugador.Jugador().showHand(); // Mostrar la mano del jugador
                for (PlayerHandler jugadorIterativo:jugadores){
                    jugadorIterativo.sendMensajeJugador("Estas son tus cartas");
                    for (Card carta : jugadorIterativo.Jugador().getMano()) {
                        jugadorIterativo.sendMensajeJugador(carta.toString());
                    }
                }

            }

            // Definir el primer turno
            System.out.println("Turno del jugador: " + jugadores.get(0).Jugador().getNombre());
            // Aquí  implementar la lógica para manejar los turnos
        }
        // Reparte cartas a los jugadores
        private void repartirCartas() {
            int cartasPorJugador = 4; // Ejemplo: cada jugador recibe 4 cartas
            for (int i = 0; i < cartasPorJugador; i++) {
                for (PlayerHandler jugador : jugadores) {
                    if (deck.getNumeroCartas() > 0) {
                        jugador.Jugador().addCard(deck.deleteCardInPosition(0));
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
            Jugador jugadorActual = jugadores.get(indiceJugadorActual).Jugador();
            System.out.println("Es el turno de: " + jugadorActual.getNombre());
        }
        private void mensajeTodosJugadores(String msg){
            for(PlayerHandler jugadoresIterados:jugadores){
                jugadoresIterados.sendMensajeJugador(msg);
            }
        }
        private void informarCompanyero(){
            jugadores.get(0).Jugador().sendMessage("Tu compañero es "+ jugadores.get(2).Jugador().getNombre());
            jugadores.get(2).Jugador().sendMessage("Tu compañero es "+ jugadores.get(0).Jugador().getNombre());
            jugadores.get(1).Jugador().sendMessage("Tu compañero es "+ jugadores.get(3).Jugador().getNombre());
            jugadores.get(3).Jugador().sendMessage("Tu compañero es "+ jugadores.get(1).Jugador().getNombre());
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

