package main;

import MD.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Juego {
        private  List<PlayerHandler> jugadores;
        private  Deck deck;
        private int indiceJugadorActual;
        private CyclicBarrier cyclicBarrier;


    // Constructor
        public Juego(List<PlayerHandler> jugadores, CyclicBarrier c) {
            // Pre: La lista de jugadores no está vacía
            // Post: Inicializa el juego con los jugadores y un mazo de cartas
            this.jugadores = new ArrayList<>(jugadores);
            this.deck = new Deck();
            this.deck.fillBarajaEspanola();
            this.indiceJugadorActual = 0;
            this.cyclicBarrier=c;
        }

        // Método para inicializar el juego

        public void iniciar() {
            int ronda;
            System.out.println("La partida ha comenzado.");
            informarCompanyero();
            //  repartir cartas a los jugadores
            deck.shuffle();
            mensajeTodosJugadores("Estas son tus cartas");
            for (PlayerHandler jugador : jugadores) {
                for (int i = 0; i < 4; i++) { // Repartir 4 cartas por jugador
                    jugador.Jugador().addCard(deck.extractCard());
                }
                // jugador.Jugador().showHand(); // Mostrar la mano del jugador
            }
            for (PlayerHandler jugadorIterativo:jugadores){
                    for (Card carta : jugadorIterativo.Jugador().getMano()) {
                        jugadorIterativo.sendMensajeJugador(carta.toString());
                    }
            }

            mensajeTodosJugadores("Configuración lista");





            //Separación por partes




            // Aquí  implementar la lógica para manejar los turnos
            while (comprobarGanador()){
                mensajeTodosJugadores("Turno del jugador: " + jugadores.get(0).Jugador().getNombre());
                if(!cortaroMus()){
                    mensajeTodosJugadores("No ha habido mus");
                }
            }
            try {
                cyclicBarrier.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }

        }
        private void apuestas(){
            mensajeTodosJugadores("hora de apostar");
            for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
                jugadores.get(i%4).sendMensajeJugador("Apuesta a Grandes");
                jugadores.get(i%4).sendMensajeJugador("Apostar o Pasar");
                jugadores.get(i%4).sendMensajeJugador("COD 23");
            }

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
        public boolean cortaroMus(){
            for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
                jugadores.get(i%4).sendMensajeJugador("Que desea hacer cortar o Mus, escribalo textualmente, sino no surtira efecto");
                jugadores.get(i%4).sendMensajeJugador("COD 23");

                String msg= jugadores.get(i%4).recibirLineaJugador();
                if(msg.equals("Mus")){
                    jugadores.get(i%4).sendMensajeJugador("OK");


                } else if(msg.equals("Cortar")){
                    jugadores.get(i%4).sendMensajeJugador("OK");
                    return false;

                }else{
                    jugadores.get(i).sendMensajeJugador("ERROR");
                }

            }
            return true;
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
        public boolean comprobarGanador(){
            return jugadores.get(0).Jugador().getPuntuacion()<=40 ||  jugadores.get(2).Jugador().getPuntuacion()<=40 ;
        }
    }

