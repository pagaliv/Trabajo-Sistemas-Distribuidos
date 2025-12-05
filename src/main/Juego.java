package main;

import MD.*;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
// Clase Juego: contiene la lógica principal de la partida
public class Juego {
    private final List<PlayerHandler> jugadores;
    private Deck deck;
    private final CyclicBarrier cyclicBarrier;
    private int indiceJugadorActual = 0;

    public Juego(List<PlayerHandler> jugadores, CyclicBarrier cyclicBarrier) {
        this.jugadores = jugadores;
        this.cyclicBarrier = cyclicBarrier;
        this.deck = new Deck();
    }

    // Inicia el bucle principal del juego
    public void jugar() {
            
            //Separación por partes




            // Aquí  implementar la lógica para manejar los turnos
            while (comprobarGanador()){
                // Notificar solo al jugador activo y avisar a los demás que esperen
                PlayerHandler activo = jugadores.get(indiceJugadorActual % jugadores.size());
                activo.sendMensajeJugador("Es tu turno: " + activo.Jugador().getNombre());
                for (PlayerHandler p : jugadores) {
                    if (p != activo) {
                        p.sendMensajeJugador("Espera tu turno; jugador actual: " + activo.Jugador().getNombre());
                    }
                }
                // Repartir las cartas al inicio de la mano (asegura que los jugadores tengan mano antes de decidir Mus/Cortar)
                repartirCartas();
                if(!cortaroMus()){
                    mensajeTodosJugadores("No ha habido mus");
                    apuestas();
                }else{
                    mensajeTodosJugadores("Habra mus");
                    repartirCartasMus();
                }


            indiceJugadorActual++;
            }
            try {
                cyclicBarrier.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }

        }
        private void repartirCartasMus() {
            for (int i = indiceJugadorActual; i < indiceJugadorActual + 4; i++) {
                jugadores.get(i % 4).sendMensajeJugador("Que cartas desea eliminar, envia su numero por posición, si son varias envialas separadas por comas");
                jugadores.get(i % 4).sendMensajeJugador("Ejemplo: Quiero enviar mi 2ºy 3º cartas, escribire: '2,3'");
                leerCartasJugador(jugadores.get(i % 4));
                String numCartasCambio=jugadores.get(i % 4).recibirLineaJugador();
                //System.out.println(numCartasCambio);
                String[] parts = numCartasCambio.split(",");
               // System.out.println(Arrays.toString(parts));
                // Convertir cada número a un entero y almacenarlo en un arreglo
                int[] numbers = Arrays.stream(parts)
                        .mapToInt(Integer::parseInt)
                        .toArray();
               // System.out.println(Arrays.toString(numbers));
                List<Card> cartas= jugadores.get(i % 4).Jugador().getMano();
                for (int number : numbers) {
                    if (!(deck.getNumeroCartas() <= 0)) {
                        cartas.set(number, deck.deleteCardInPosition(0));
                    } else {
                        deck = new Deck();
                        for (PlayerHandler player : jugadores) {
                            for (Card cartaParaEliminar : player.Jugador().getMano()) {
                                deck.deleteCard(cartaParaEliminar);
                            }
                        }
                        cartas.set(number, deck.deleteCardInPosition(0));
                    }

                }



                leerCartasJugador(jugadores.get(i % 4));
            }
        }
        private void leerCartasJugador(PlayerHandler ph){
            for(int i=0;i<4;i++){
               ph.sendMensajeJugador(i+". " + ph.Jugador().getMano().get(i).toString());
            }
            ph.sendMensajeJugador("COD 23");
        }
        private void apuestas(){
            int jugadorOrdago=-1;
            boolean ordago=false;
            boolean apostadoenJuego=false;
            String paloOrdago="";
            int[] apuestagrandes = new int[4];    // Inicializa con 4 elementos, todos iguales a 0
            int[] apuestapequenyas = new int[4];   // Inicializa con 4 elementos, todos iguales a 0
            int[] apuestaPares = new int[4];       // Inicializa con 4 elementos, todos iguales a 0
            int[] apuestaJuego = new int[4];       // Inicializa con 4 elementos, todos iguales a 0
            int[] apuestaPunto = new int[4];       // Inicializa con 4 elementos, todos iguales a 0

            mensajeTodosJugadores("hora de apostar");
            for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
                if(!ordago){
                    PlayerHandler ph = jugadores.get(i%4);
                    Jugador j = ph.Jugador();
                    int puntosActuales = j.getPuntuacion();
                    int puntosNecesarios = 40 - puntosActuales; // apostar esto equivale a ordago
                    int minApuesta = 2;
                    int maxApuesta = puntosNecesarios;
                    // Si el número necesario para ganar es menor que el mínimo, forzamos que min==max para indicar ordago posible
                    if (maxApuesta < minApuesta) {
                        minApuesta = maxApuesta;
                    }

                    // Mensaje claro de instrucciones
                    ph.sendMensajeJugador("Apuesta a Grandes: puedes apostar entre " + minApuesta + " y " + maxApuesta + ".");
                    ph.sendMensajeJugador("Para apostar escribe 'Apostar' y en la siguiente línea la cantidad; para pasar escribe 'Pasar'.");
                    ph.sendMensajeJugador("Apostar o Pasar");
                    apuestasEquipoContrario(i,apuestagrandes);
                    ph.sendMensajeJugador("COD 23");

                    String msg = ph.recibirLineaJugador();
                    if (msg == null) {
                        ph.sendMensajeJugador("ERROR");
                        continue;
                    }

                    if(msg.equalsIgnoreCase("Apostar")){
                        ph.sendMensajeJugador("OK");
                        // Cuánto va a apostar (leer siguiente línea)
                        String montoStr = ph.recibirLineaJugador();
                        if (montoStr == null || montoStr.isEmpty()) {
                            ph.sendMensajeJugador("ERROR");
                            continue;
                        }
                        int monto;
                        try {
                            monto = Integer.parseInt(montoStr.trim());
                        } catch (NumberFormatException e) {
                            ph.sendMensajeJugador("ERROR");
                            continue;
                        }

                        // Si apuesta >= puntosNecesarios -> ordago
                        if (monto >= puntosNecesarios) {
                            ph.sendMensajeJugador("COD 28");
                            ordago = true;
                            jugadorOrdago = i % 4;
                            paloOrdago = "Grandes";
                            // Notificar adversarios explícitamente
                            jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a Grandes (COD 28)");
                            jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a Grandes (COD 28)");

                        } else if (monto >= minApuesta && monto < puntosNecesarios) {
                            ph.sendMensajeJugador("COD 19");
                            apuestagrandes[i%4] = monto + CantidadapuestasEquipoContrario(i,apuestagrandes);

                        } else {
                            ph.sendMensajeJugador("ERROR");
                        }

                    } else if(msg.equalsIgnoreCase("Pasar")){
                        ph.sendMensajeJugador("OK");

                    } else {
                        ph.sendMensajeJugador("ERROR");
                    }
                }
            }
            if(!ordago){
                int m=posicionJugadorConCartasMasGrandes(jugadores);
                jugadores.get(m).Jugador().sumarPuntos(Arrays.stream(apuestagrandes).max().getAsInt());
                jugadores.get((m + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestagrandes).max().getAsInt());

            }
            for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
                if(!ordago){
                    PlayerHandler ph = jugadores.get(i%4);
                    Jugador j = ph.Jugador();
                    int puntosActuales = j.getPuntuacion();
                    int puntosNecesarios = 40 - puntosActuales;
                    int minApuesta = 2;
                    int maxApuesta = puntosNecesarios;
                    if (maxApuesta < minApuesta) minApuesta = maxApuesta;

                    ph.sendMensajeJugador("Apuesta a Pequenyas: puedes apostar entre " + minApuesta + " y " + maxApuesta + ".");
                    ph.sendMensajeJugador("Para apostar escribe 'Apostar' y en la siguiente línea la cantidad; para pasar escribe 'Pasar'.");
                    ph.sendMensajeJugador("Apostar o Pasar");
                    apuestasEquipoContrario(i,apuestapequenyas);
                    ph.sendMensajeJugador("COD 23");

                    String msg = ph.recibirLineaJugador();
                    if (msg == null) { ph.sendMensajeJugador("ERROR"); continue; }

                    if(msg.equalsIgnoreCase("Apostar")){
                        ph.sendMensajeJugador("OK");
                        String montoStr = ph.recibirLineaJugador();
                        if (montoStr == null || montoStr.isEmpty()) { ph.sendMensajeJugador("ERROR"); continue; }
                        int monto;
                        try { monto = Integer.parseInt(montoStr.trim()); } catch (NumberFormatException e){ ph.sendMensajeJugador("ERROR"); continue; }

                        if (monto >= puntosNecesarios) {
                            ph.sendMensajeJugador("COD 28");
                            ordago = true;
                            jugadorOrdago = i%4;
                            paloOrdago = "Pequenyas";
                            jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a Pequenyas (COD 28)");
                            jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a Pequenyas (COD 28)");
                        } else if (monto >= minApuesta) {
                            ph.sendMensajeJugador("COD 19");
                            apuestapequenyas[i%4] = monto + CantidadapuestasEquipoContrario(i,apuestapequenyas);
                        } else { ph.sendMensajeJugador("ERROR"); }

                    } else if(msg.equalsIgnoreCase("Pasar")){
                        ph.sendMensajeJugador("OK");
                    } else { ph.sendMensajeJugador("ERROR"); }
                }
            }
            if(!ordago){
                int m=posicionJugadorConCartasMasGrandes(jugadores);
                jugadores.get(m).Jugador().sumarPuntos(Arrays.stream(apuestapequenyas).max().getAsInt());
                jugadores.get((m + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestapequenyas).max().getAsInt());

            }

            for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
                if(!ordago){
                    PlayerHandler ph = jugadores.get(i%4);
                    Jugador j = ph.Jugador();
                    int puntosActuales = j.getPuntuacion();
                    int puntosNecesarios = 40 - puntosActuales;
                    int minApuesta = 2;
                    int maxApuesta = puntosNecesarios;
                    if (maxApuesta < minApuesta) minApuesta = maxApuesta;

                    ph.sendMensajeJugador("Apuesta a Pares: puedes apostar entre " + minApuesta + " y " + maxApuesta + ".");
                    ph.sendMensajeJugador("Para apostar escribe 'Apostar' y en la siguiente línea la cantidad; para pasar escribe 'Pasar'.");
                    ph.sendMensajeJugador("Apostar o Pasar");
                    apuestasEquipoContrario(i,apuestaPares);
                    ph.sendMensajeJugador("COD 23");

                    String msg = ph.recibirLineaJugador();
                    if (msg == null) { ph.sendMensajeJugador("ERROR"); continue; }

                    if(msg.equalsIgnoreCase("Apostar")){
                        ph.sendMensajeJugador("OK");
                        String montoStr = ph.recibirLineaJugador();
                        if (montoStr == null || montoStr.isEmpty()) { ph.sendMensajeJugador("ERROR"); continue; }
                        int monto;
                        try { monto = Integer.parseInt(montoStr.trim()); } catch (NumberFormatException e){ ph.sendMensajeJugador("ERROR"); continue; }

                        if (monto >= puntosNecesarios) {
                            ph.sendMensajeJugador("COD 28");
                            ordago = true;
                            jugadorOrdago = i%4;
                            paloOrdago = "Pares";
                            jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a Pares (COD 28)");
                            jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a Pares (COD 28)");
                        } else if (monto >= minApuesta) {
                            ph.sendMensajeJugador("COD 19");
                            apuestaPares[i%4] = monto + CantidadapuestasEquipoContrario(i,apuestaPares);
                        } else { ph.sendMensajeJugador("ERROR"); }

                    } else if(msg.equalsIgnoreCase("Pasar")){
                        ph.sendMensajeJugador("OK");
                    } else { ph.sendMensajeJugador("ERROR"); }
                }
            }
            if(!ordago){
                int m=posicionJugadorConCartasMasGrandes(jugadores);
                jugadores.get(m).Jugador().sumarPuntos(Arrays.stream(apuestaPares).max().getAsInt());
                jugadores.get((m + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestaPares).max().getAsInt());

            }
            // Procesar fase Juego mediante helper (mínimo 3)
            if (!ordago) {
                int ord = procesarFaseApuesta("Juego", apuestaJuego, 3, "Juego");
                if (ord != -1) {
                    ordago = true;
                    jugadorOrdago = ord;
                    paloOrdago = "Juego";
                } else {
                    int m3 = posicionJugadorConCartasMasGrandes(jugadores);
                    jugadores.get(m3).Jugador().sumarPuntos(Arrays.stream(apuestaJuego).max().getAsInt());
                    jugadores.get((m3 + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestaJuego).max().getAsInt());
                }
            }
            if(!apostadoenJuego){
            // Procesar fase al Punto mediante helper (mínimo 3)
            if (!ordago) {
                int ord = procesarFaseApuesta("Punto", apuestaPunto, 3, "Punto");
                if (ord != -1) {
                    ordago = true;
                    jugadorOrdago = ord;
                    paloOrdago = "Punto";
                } else {
                    int m4 = posicionJugadorConCartasMasGrandes(jugadores);
                    jugadores.get(m4).Jugador().sumarPuntos(Arrays.stream(apuestaPunto).max().getAsInt());
                    jugadores.get((m4 + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestaPunto).max().getAsInt());
                }
            }
            }



            if(ordago) {
                mensajeTodosJugadores("El jugador " + jugadores.get(jugadorOrdago).Jugador().getNombre() + "ha hecho un ordago a " + paloOrdago);
                jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("Desea 'aceptar ordago' o 'no aceptar' ");
                jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("Desea 'aceptar ordago' o 'no aceptar'");
                mensajeTodosJugadores("COD 23");
                //De este modo no es eficiente porque tendran que esperar a que ambos jugadores respondan
                String resp1 = jugadores.get((jugadorOrdago + 1) % 4).recibirLineaJugador();
                String resp2 = jugadores.get((jugadorOrdago + 3) % 4).recibirLineaJugador();
                         if (resp1 != null && resp1.equalsIgnoreCase("aceptar ordago")) {
                    int ganador;
                    jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("OK");
                    switch (paloOrdago) {
                        case "Grandes" -> {
                            ganador = posicionJugadorConCartasMasGrandes(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        case "Pequenyas" -> {
                            ganador = posicionJugadorConCartasMasPequenas(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        case "Pares" -> {
                            ganador = posicionJugadorConMejorPar(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        case "Juego" -> {
                            ganador = posicionJugadorConMejorJuego(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        default -> {
                            ganador = posicionJugadorMasCercaDe30(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);

                        }
                    }
                    mensajeTodosJugadores("Ha ganado el equipo de: " + jugadores.get(ganador).Jugador().getNombre() + " y " + jugadores.get(ganador).Jugador().getNombre());


                } else {
                    jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("ERROR");
                }
                    if (resp2 != null && resp2.equalsIgnoreCase("aceptar ordago")) {
                    int ganador;
                    jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("OK");
                    switch (paloOrdago) {
                        case "Grandes" -> {
                            ganador = posicionJugadorConCartasMasGrandes(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        case "Pequenyas" -> {
                            ganador = posicionJugadorConCartasMasPequenas(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        case "Pares" -> {
                            ganador = posicionJugadorConMejorPar(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        case "Juego" -> {
                            ganador = posicionJugadorConMejorJuego(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                        }
                        default -> {
                            ganador = posicionJugadorMasCercaDe30(jugadores);
                            jugadores.get(ganador).Jugador().sumarPuntos(40);
                            jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);

                        }
                    }
                    mensajeTodosJugadores("Ha ganado el equipo de: " + jugadores.get(ganador).Jugador().getNombre() + " y " + jugadores.get(ganador).Jugador().getNombre());


                } else {
                    jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("ERROR");
                }
            }







        }





        // Reparte cartas a los jugadores
        private void repartirCartas() {
            int cartasPorJugador = 4; // Ejemplo: cada jugador recibe 4 cartas
            // Limpiar manos previas antes de repartir
            for (PlayerHandler ph : jugadores) {
                ph.Jugador().descartarCartas();
            }

            for (int i = 0; i < cartasPorJugador; i++) {
                for (PlayerHandler jugador : jugadores) {
                    if (deck.getNumeroCartas() > 0) {
                        jugador.Jugador().addCard(deck.deleteCardInPosition(0));
                    } else {
                        deck = new Deck();
                        for (PlayerHandler player : jugadores) {
                            for (Card cartaParaEliminar : player.Jugador().getMano()) {
                                deck.deleteCard(cartaParaEliminar);
                            }
                        }
                        jugador.Jugador().addCard(deck.deleteCardInPosition(0));
                    }
                }
            }

            // Enviar la mano a cada jugador para que la vea en su cliente
            for (PlayerHandler ph : jugadores) {
                ph.sendMensajeJugador("Tus cartas son:");
                leerCartasJugador(ph);
            }
        }
        public boolean cortaroMus(){
            for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
                PlayerHandler ph = jugadores.get(i % 4);
                ph.sendMensajeJugador("Indica 'M' para Mus o 'C' para Cortar (una sola letra, mayúscula o minúscula aceptada).");
                
                String msg = ph.recibirLineaJugador();
                
                java.util.logging.Logger.getLogger(Juego.class.getName()).info(String.valueOf(msg));
                if (msg == null) {
                    // Si el jugador se desconectó o no respondió, tratamos como error y seguimos
                    ph.sendMensajeJugador("ERROR");
                    continue;
                }

                if (msg.equalsIgnoreCase("Mus") || msg.equalsIgnoreCase("M")) {
                    ph.sendMensajeJugador("OK");

                } else if (msg.equalsIgnoreCase("Cortar") || msg.equalsIgnoreCase("C")) {
                    ph.sendMensajeJugador("OK");
                    // Informar a todos los jugadores que se ha cortado el Mus para que queden sincronizados
                    mensajeTodosJugadores("Mus cortado por " + ph.Jugador().getNombre());
                    return false;

                } else {
                    ph.sendMensajeJugador("ERROR");
                }
                ph.sendMensajeJugador("COD 23");
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
            // Devuelve true mientras ningún equipo haya alcanzado 40 puntos
            return jugadores.get(0).Jugador().getPuntuacion() < 40 && jugadores.get(2).Jugador().getPuntuacion() < 40;
        }
    public int posicionJugadorConCartasMasGrandes(List<PlayerHandler> jugadores) {
        int posicionGanador = -1;
        int maxValorMano = Integer.MIN_VALUE;

        for (int i = 0; i < jugadores.size(); i++) {
            // Obtener las cartas del jugador actual
            List<Card> mano = jugadores.get(i).Jugador().getMano();

            // Calcular el valor total de las cartas
            int valorMano = mano.stream().mapToInt(Card::getCardNum).sum();

            // Verificar si es la mano más grande encontrada hasta ahora
            if (valorMano > maxValorMano) {
                maxValorMano = valorMano;
                posicionGanador = i;
            }
        }

        return posicionGanador;
    }
    public int posicionJugadorConCartasMasPequenas(List<PlayerHandler> jugadores) {
        int posicionGanador = -1;
        int minValorMano = Integer.MAX_VALUE;

        for (int i = 0; i < jugadores.size(); i++) {
            // Obtener las cartas del jugador actual
            List<Card> mano = jugadores.get(i).Jugador().getMano();

            // Calcular el valor total de las cartas
            int valorMano = mano.stream().mapToInt(Card::getCardNum).sum();

            // Verificar si es la mano más pequeña encontrada hasta ahora
            if (valorMano < minValorMano) {
                minValorMano = valorMano;
                posicionGanador = i;
            }
        }

        return posicionGanador;
    }
    public int posicionJugadorConMejorPar(List<PlayerHandler> jugadores) {
        int posicionGanador = -1;
        int mejorJerarquia = -1;
        int mayorValorJerarquia = -1;

        for (int i = 0; i < jugadores.size(); i++) {
            PlayerHandler jugador = jugadores.get(i);

            // Obtener la mano del jugador
            List<Card> mano = jugador.Jugador().getMano();

            // Contar las cartas por valor
            Map<Integer, Integer> conteoCartas = new HashMap<>();
            for (Card carta : mano) {
                conteoCartas.put(carta.getCardNum(), conteoCartas.getOrDefault(carta.getCardNum(), 0) + 1);
            }

            // Evaluar la jerarquía
            int jerarquia = 0; // 4: cuarteto, 3: trío, 2: doble pareja, 1: pareja, 0: nada
            int valorJerarquia = 0; // Valor más alto entre las combinaciones
            int parejas = 0;

            for (Map.Entry<Integer, Integer> entry : conteoCartas.entrySet()) {
                int valor = entry.getKey();
                int cantidad = entry.getValue();

                if (cantidad == 4) {
                    jerarquia = 4;
                    valorJerarquia = valor;
                    break; // Cuarteto es la jerarquía más alta, no necesitamos seguir
                } else if (cantidad == 3) {
                    if (jerarquia < 3) {
                        jerarquia = 3;
                        valorJerarquia = valor;
                    }
                } else if (cantidad == 2) {
                    parejas++;
                    if (parejas == 2 && jerarquia < 2) {
                        jerarquia = 2;
                        valorJerarquia = Math.max(valorJerarquia, valor); // Usar el valor mayor entre las parejas
                    } else if (jerarquia < 1) {
                        jerarquia = 1;
                        valorJerarquia = valor;
                    }
                }
            }

            // Comparar con el mejor jugador encontrado hasta ahora
            if (jerarquia > mejorJerarquia || (jerarquia == mejorJerarquia && valorJerarquia > mayorValorJerarquia)) {
                mejorJerarquia = jerarquia;
                mayorValorJerarquia = valorJerarquia;
                posicionGanador = i;
            }
        }

        return posicionGanador;
    }
    public int posicionJugadorConMejorJuego(List<PlayerHandler> jugadores) {
        int posicionGanador = -1;
        int mejorJuego = -1;

        for (int i = 0; i < jugadores.size(); i++) {
            PlayerHandler jugador = jugadores.get(i);

            // Obtener la mano del jugador
            List<Card> mano = jugador.Jugador().getMano();

            // Calcular el valor del juego
            int juego = calcularJuego(mano);

            // Comparar con el mejor juego encontrado
            if (juego > mejorJuego) {
                mejorJuego = juego;
                posicionGanador = i;
            }
        }

        return posicionGanador;
    }

    private int calcularJuego(List<Card> mano) {
        // Asignar valores al juego en base al Mus
        Map<Integer, Integer> valoresCartas = Map.of(
                8, 10, // Figuras (Rey, Reina, Sota)
                9, 10,
                10, 10,
                1, 1,
                2, 2,
                3, 3,
                4, 4,
                5, 5,
                6, 6,
                7, 7
        );

        // Calcular la suma de las dos cartas más altas
        List<Integer> valoresMano = mano.stream()
                .map(Card::getCardNum)
                .map(valoresCartas::get)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int suma = valoresMano.get(0) + valoresMano.get(1);

        // Jerarquía de valores del Mus
        List<Integer> jerarquia = Arrays.asList(31, 32, 40, 39, 38, 37, 36, 35, 34, 33);

        // Determinar si está en la jerarquía
        if (jerarquia.contains(suma)) {
            return jerarquia.indexOf(suma); // Cuanto más alto, mejor posición
        }

        return -1; // Si no está en la jerarquía, es un juego no competitivo
    }
    public int posicionJugadorMasCercaDe30(List<PlayerHandler> jugadores) {
        int posicionMejorCercaDe30 = -1;
        int mejorDiferencia = Integer.MAX_VALUE;

        for (int i = 0; i < jugadores.size(); i++) {
            PlayerHandler jugador = jugadores.get(i);

            // Obtener la mano del jugador
            List<Card> mano = jugador.Jugador().getMano();

            // Calcular el valor del juego
            int juego = calcularJuego(mano);

            // Verificar si el juego es válido y si se acerca a 30
            if (juego <= 30 && juego >= 0) {
                int diferencia = 30 - juego;

                // Si la diferencia con 30 es menor que la mejor diferencia encontrada, actualizar
                if (diferencia < mejorDiferencia) {
                    mejorDiferencia = diferencia;
                    posicionMejorCercaDe30 = i;
                }
            }
        }

        return posicionMejorCercaDe30;
    }
    private void apuestasEquipoContrario(int i,int[] apuestas){
        if(apuestas[(i+1)%4]!=0){
            jugadores.get(i%4).sendMensajeJugador("El jugador "+  jugadores.get((i+1)%4).Jugador().getNombre() + "ha apostado " + apuestas[(i+1)%4]);
        }
        if(apuestas[(i+3)%4]!=0){
            jugadores.get(i%4).sendMensajeJugador("El jugador "+  jugadores.get((i+3)%4).Jugador().getNombre() + "ha apostado " + apuestas[(i+3)%4]);
        }


    }
    private int CantidadapuestasEquipoContrario(int i,int[] apuestas){
        int n=apuestas[(i+1)%4];

        if(apuestas[(i+3)%4]>n){
           n=apuestas[(i+3)%4];
        }
        return n;


    }

    /**
     * Procesa una fase de apuesta (Grandes, Pequenyas, Pares, Juego, Punto) de forma genérica.
     * Devuelve el índice del jugador que hizo ordago (>= puntosNecesarios) o -1 si no hubo ordago.
     */
    private int procesarFaseApuesta(String faseNombre, int[] apuestas, int minApuestaParam, String palo) {
        for (int i = indiceJugadorActual; i < indiceJugadorActual + 4; i++) {
            PlayerHandler ph = jugadores.get(i % 4);
            Jugador j = ph.Jugador();
            int puntosActuales = j.getPuntuacion();
            int puntosNecesarios = 40 - puntosActuales;
            int minApuesta = Math.max(2, minApuestaParam);
            int maxApuesta = puntosNecesarios;
            if (maxApuesta < minApuesta) minApuesta = maxApuesta;

            ph.sendMensajeJugador("Apuesta a " + faseNombre + ": puedes apostar entre " + minApuesta + " y " + maxApuesta + ".");
            ph.sendMensajeJugador("Para apostar escribe 'Apostar' y en la siguiente línea la cantidad; para pasar escribe 'Pasar'.");
            ph.sendMensajeJugador("Apostar o Pasar");
            apuestasEquipoContrario(i, apuestas);
            ph.sendMensajeJugador("COD 23");

            String msg = ph.recibirLineaJugador();
            if (msg == null) {
                ph.sendMensajeJugador("ERROR");
                continue;
            }

            if (msg.equalsIgnoreCase("Apostar")) {
                ph.sendMensajeJugador("OK");
                String montoStr = ph.recibirLineaJugador();
                if (montoStr == null || montoStr.isEmpty()) { ph.sendMensajeJugador("ERROR"); continue; }
                int monto;
                try { monto = Integer.parseInt(montoStr.trim()); } catch (NumberFormatException e){ ph.sendMensajeJugador("ERROR"); continue; }

                if (monto >= puntosNecesarios) {
                    // ordago
                    ph.sendMensajeJugador("COD 28");
                    // notify adversaries
                    jugadores.get((i % 4 + 1) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a " + palo + " (COD 28)");
                    jugadores.get((i % 4 + 3) % 4).sendMensajeJugador("El jugador " + j.getNombre() + " ha hecho ORDAGO a " + palo + " (COD 28)");
                    return i % 4;
                } else if (monto >= minApuesta) {
                    ph.sendMensajeJugador("COD 19");
                    apuestas[i%4] = monto + CantidadapuestasEquipoContrario(i, apuestas);
                } else {
                    ph.sendMensajeJugador("ERROR");
                }

            } else if (msg.equalsIgnoreCase("Pasar")) {
                ph.sendMensajeJugador("OK");
            } else {
                ph.sendMensajeJugador("ERROR");
            }
        }
        return -1;
    }





}

