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
        // Aquí implementar la lógica para manejar los turnos
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

    private void leerCartasJugador(PlayerHandler ph){
        for(int i=0;i<4;i++){
            ph.sendMensajeJugador(i+". " + ph.Jugador().getMano().get(i).toString());
        }
        ph.sendMensajeJugador("COD 23");
    }

    private void repartirCartasMus() {
        for (int i = indiceJugadorActual; i < indiceJugadorActual + 4; i++) {
            PlayerHandler ph = jugadores.get(i % 4);
            ph.sendMensajeJugador("Que cartas desea eliminar, envia su numero por posición, si son varias envialas separadas por comas");
            ph.sendMensajeJugador("Ejemplo: Quiero enviar mi 2ºy 3º cartas, escribire: '2,3'");
            leerCartasJugador(ph);
            String numCartasCambio = ph.recibirLineaJugador();
            if (numCartasCambio == null || numCartasCambio.trim().isEmpty()) {
                ph.sendMensajeJugador("ERROR");
                continue;
            }
            String[] parts = numCartasCambio.split(",");
            int[] numbers;
            try {
                numbers = Arrays.stream(parts).mapToInt(s -> {
                    try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
                }).filter(n -> n >= 0 && n < 4).toArray();
            } catch (Exception e) {
                ph.sendMensajeJugador("ERROR");
                continue;
            }
            List<Card> cartas = ph.Jugador().getMano();
            for (int number : numbers) {
                if (deck.getNumeroCartas() <= 0) {
                    deck = new Deck();
                    for (PlayerHandler player : jugadores) {
                        for (Card cartaParaEliminar : player.Jugador().getMano()) {
                            deck.deleteCard(cartaParaEliminar);
                        }
                    }
                }
                cartas.set(number, deck.deleteCardInPosition(0));
            }

            leerCartasJugador(ph);
        }
    }

    private void apuestas() {
        int jugadorOrdago=-1;
        boolean ordago=false;
        boolean ordagoResuelto=false; // evita doble adjudicación si ambos aceptan
        boolean apostadoenJuego=false;
        String paloOrdago="";
        int[] apuestagrandes = new int[4];
        int[] apuestapequenyas = new int[4];
        int[] apuestaPares = new int[4];
        int[] apuestaJuego = new int[4];
        int[] apuestaPunto = new int[4];

        // Fase: Grandes
        boolean grandesAccepted = false;
        for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
            if(!ordago){
                PlayerHandler ph = jugadores.get(i%4);
                Jugador j = ph.Jugador();
                int puntosActuales = j.getPuntuacion();
                int puntosNecesarios = 40 - puntosActuales;
                int minApuesta = 2;
                int maxApuesta = puntosNecesarios;
                if (maxApuesta < minApuesta) minApuesta = maxApuesta;

                ph.sendMensajeJugador("Apuesta a Grandes: puedes apostar entre " + minApuesta + " y " + maxApuesta + ".");
                ph.sendMensajeJugador("Para apostar escribe 'Apostar' y en la siguiente línea la cantidad; para pasar escribe 'Pasar'.");
                ph.sendMensajeJugador("Apostar o Pasar");
                apuestasEquipoContrario(i,apuestagrandes);
                ph.sendMensajeJugador("COD 23");

                String msg = ph.recibirLineaJugador();
                if (msg == null) { ph.sendMensajeJugador("ERROR"); continue; }
                if (msg.equalsIgnoreCase("Retirarse") || msg.equalsIgnoreCase("Retirar") || msg.equalsIgnoreCase("Fold")) {
                    boolean resolved = handleFold(i%4, apuestagrandes);
                    if (resolved) return;
                } else if (msg.toLowerCase().startsWith("apostar") || msg.matches("\\d+")) {
                    String montoStr = null;
                    String lower = msg.trim();
                    String[] tokens = lower.split("\\s+");
                    if (tokens.length >= 2 && tokens[0].equalsIgnoreCase("apostar")) {
                        montoStr = tokens[1];
                    } else if (tokens.length == 1 && tokens[0].matches("\\d+")) {
                        montoStr = tokens[0];
                    } else if (tokens.length == 1 && tokens[0].equalsIgnoreCase("apostar")) {
                        ph.sendMensajeJugador("OK");
                        montoStr = ph.recibirLineaJugador();
                    }

                    if (montoStr == null || montoStr.trim().isEmpty()) { ph.sendMensajeJugador("ERROR"); continue; }
                    int monto;
                    try { monto = Integer.parseInt(montoStr.trim()); } catch (NumberFormatException e){ ph.sendMensajeJugador("ERROR"); continue; }

                    if (monto >= puntosNecesarios) {
                        ph.sendMensajeJugador("COD 28");
                        ordago = true;
                        jugadorOrdago = i%4;
                        paloOrdago = "Grandes";
                        jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("ORDAGO: El jugador " + j.getNombre() + " ha hecho ORDAGO a Grandes. Responda 'aceptar ordago' o 'no aceptar' (COD 28)");
                        jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("ORDAGO: El jugador " + j.getNombre() + " ha hecho ORDAGO a Grandes. Responda 'aceptar ordago' o 'no aceptar' (COD 28)");
                    } else if (monto >= minApuesta) {
                        ph.sendMensajeJugador("COD 19");
                        apuestagrandes[i%4] = monto + CantidadapuestasEquipoContrario(i,apuestagrandes);
                        // Notificar y preguntar a adversarios si aceptan la apuesta
                        boolean acept = promptAdversariosAceptarApuesta(i%4, monto);
                        if (acept) {
                            mensajeTodosJugadores("La apuesta de " + j.getNombre() + " de " + monto + " ha sido aceptada.");
                            grandesAccepted = true;
                            break; // avanzar a la siguiente fase
                        }
                    } else { ph.sendMensajeJugador("ERROR"); }

                } else if(msg.equalsIgnoreCase("Pasar")){
                    ph.sendMensajeJugador("OK");
                } else { ph.sendMensajeJugador("ERROR"); }
            }
        }

        // Después de recorrer Grandes, adjudicar puntos si no hay ordago and nobody accepted
        if(!ordago){
            if (!grandesAccepted) {
                int m=posicionJugadorConCartasMasGrandes(jugadores);
                jugadores.get(m).Jugador().sumarPuntos(Arrays.stream(apuestagrandes).max().orElse(0));
                jugadores.get((m + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestagrandes).max().orElse(0));
            } else {
                mensajeTodosJugadores("La apuesta a Grandes fue aceptada; avanzando a Pequenyas.");
            }
        }

        // Fase: Pequenyas
        boolean pequeAccepted = false;
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
                if (msg.equalsIgnoreCase("Retirarse") || msg.equalsIgnoreCase("Retirar") || msg.equalsIgnoreCase("Fold")) {
                    boolean resolved = handleFold(i%4, apuestapequenyas);
                    if (resolved) return;
                } else if (msg.toLowerCase().startsWith("apostar") || msg.matches("\\d+")) {
                    String montoStr = null;
                    String lower = msg.trim();
                    String[] tokens = lower.split("\\s+");
                    if (tokens.length >= 2 && tokens[0].equalsIgnoreCase("apostar")) {
                        montoStr = tokens[1];
                    } else if (tokens.length == 1 && tokens[0].matches("\\d+")) {
                        montoStr = tokens[0];
                    } else if (tokens.length == 1 && tokens[0].equalsIgnoreCase("apostar")) {
                        ph.sendMensajeJugador("OK");
                        montoStr = ph.recibirLineaJugador();
                    }

                    if (montoStr == null || montoStr.trim().isEmpty()) { ph.sendMensajeJugador("ERROR"); continue; }
                    int monto;
                    try { monto = Integer.parseInt(montoStr.trim()); } catch (NumberFormatException e){ ph.sendMensajeJugador("ERROR"); continue; }

                    if (monto >= puntosNecesarios) {
                        ph.sendMensajeJugador("COD 28");
                        ordago = true;
                        jugadorOrdago = i%4;
                        paloOrdago = "Pequenyas";
                        jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("ORDAGO: El jugador " + j.getNombre() + " ha hecho ORDAGO a Pequenyas. Responda 'aceptar ordago' o 'no aceptar' (COD 28)");
                        jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("ORDAGO: El jugador " + j.getNombre() + " ha hecho ORDAGO a Pequenyas. Responda 'aceptar ordago' o 'no aceptar' (COD 28)");
                    } else if (monto >= minApuesta) {
                        ph.sendMensajeJugador("COD 19");
                        apuestapequenyas[i%4] = monto + CantidadapuestasEquipoContrario(i,apuestapequenyas);
                        boolean acept2 = promptAdversariosAceptarApuesta(i%4, monto);
                        if (acept2) {
                            mensajeTodosJugadores("La apuesta de " + j.getNombre() + " de " + monto + " ha sido aceptada.");
                            pequeAccepted = true;
                            break;
                        }
                    } else { ph.sendMensajeJugador("ERROR"); }

                } else if(msg.equalsIgnoreCase("Pasar")){
                    ph.sendMensajeJugador("OK");
                } else { ph.sendMensajeJugador("ERROR"); }
            }
        }
        if(!ordago){
            if (!pequeAccepted) {
                int m=posicionJugadorConCartasMasGrandes(jugadores);
                jugadores.get(m).Jugador().sumarPuntos(Arrays.stream(apuestaPares).max().getAsInt());
                jugadores.get((m + 2) % 4).Jugador().sumarPuntos(Arrays.stream(apuestaPares).max().getAsInt());
            } else {
                mensajeTodosJugadores("La apuesta a Pequenyas fue aceptada; avanzando a la siguiente fase.");
            }
        }

        // Procesar fase Juego mediante helper (mínimo 3)
        if (!ordago) {
            int ord = procesarFaseApuesta("Juego", apuestaJuego, 3, "Juego");
            if (ord == -2) {
                // fold resolved inside procesarFaseApuesta
                return;
            } else if (ord == -3) {
                // apuesta aceptada: avanzamos a la siguiente fase sin adjudicar aquí
                mensajeTodosJugadores("Apuesta en Juego aceptada; avanzando a Punto.");
            } else if (ord != -1) {
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
                if (ord == -2) {
                    return;
                } else if (ord == -3) {
                    mensajeTodosJugadores("Apuesta en Punto aceptada; continuando.");
                } else if (ord != -1) {
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
            mensajeTodosJugadores("El jugador " + jugadores.get(jugadorOrdago).Jugador().getNombre() + " ha hecho un ordago a " + paloOrdago);
            jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("Desea 'aceptar ordago' o 'no aceptar' ");
            jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("Desea 'aceptar ordago' o 'no aceptar'");
            mensajeTodosJugadores("COD 23");
            //De este modo no es eficiente porque tendran que esperar a que ambos jugadores respondan
            String resp1 = jugadores.get((jugadorOrdago + 1) % 4).recibirLineaJugador();
            String resp2 = jugadores.get((jugadorOrdago + 3) % 4).recibirLineaJugador();
            boolean anyAccepted = false;
            int ganador = -1;
            if (resp1 != null && resp1.equalsIgnoreCase("aceptar ordago")) {
                jugadores.get((jugadorOrdago + 1) % 4).sendMensajeJugador("OK");
                anyAccepted = true;
            }
            if (resp2 != null && resp2.equalsIgnoreCase("aceptar ordago")) {
                jugadores.get((jugadorOrdago + 3) % 4).sendMensajeJugador("OK");
                anyAccepted = true;
            }
            if (anyAccepted) {
                switch (paloOrdago) {
                    case "Grandes" -> ganador = posicionJugadorConCartasMasGrandes(jugadores);
                    case "Pequenyas" -> ganador = posicionJugadorConCartasMasPequenas(jugadores);
                    case "Pares" -> ganador = posicionJugadorConMejorPar(jugadores);
                    case "Juego" -> ganador = posicionJugadorConMejorJuego(jugadores);
                    default -> ganador = posicionJugadorMasCercaDe30(jugadores);
                }
                jugadores.get(ganador).Jugador().sumarPuntos(40);
                jugadores.get((ganador + 2) % 4).Jugador().sumarPuntos(40);
                mensajeTodosJugadores("Ha ganado el equipo de: " + jugadores.get(ganador).Jugador().getNombre() + " y " + jugadores.get((ganador+2)%4).Jugador().getNombre());
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
        // Persist a lightweight snapshot after dealing hands so server operators can inspect state.
        try {
            GamePersistence.saveSnapshot(this, "game_snapshot.xml");
        } catch (Exception e) {
            System.err.println("Error saving game snapshot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Rest of helpers
    public java.util.List<PlayerHandler> getJugadores() {
        return this.jugadores;
    }

    public int getIndiceJugadorActual() {
        return this.indiceJugadorActual;
    }

    public boolean cortaroMus(){
        for(int i=indiceJugadorActual; i<indiceJugadorActual+4;i++){
            PlayerHandler ph = jugadores.get(i % 4);
            ph.sendMensajeJugador("Indica 'M' para Mus o 'C' para Cortar (una sola letra, mayúscula o minúscula aceptada).");

            String msg = ph.recibirLineaJugador();
            java.util.logging.Logger.getLogger(Juego.class.getName()).info(String.valueOf(msg));
            if (msg == null) {
                ph.sendMensajeJugador("ERROR");
                continue;
            }

            if (msg.equalsIgnoreCase("Mus") || msg.equalsIgnoreCase("M")) {
                ph.sendMensajeJugador("OK");
            } else if (msg.equalsIgnoreCase("Cortar") || msg.equalsIgnoreCase("C")) {
                ph.sendMensajeJugador("OK");
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
        System.out.println(jugador.getNombre() + " realiza la acción: " + accion);
        pasarTurno();
    }

    // Método para finalizar el juego
    public void finalizarJuego() {
        System.out.println("El juego ha terminado.");
    }

    public boolean comprobarGanador(){
        return jugadores.get(0).Jugador().getPuntuacion() < 40 && jugadores.get(2).Jugador().getPuntuacion() < 40;
    }

    public int posicionJugadorConCartasMasGrandes(List<PlayerHandler> jugadores) {
        int posicionGanador = -1;
        int maxValorMano = Integer.MIN_VALUE;
        for (int i = 0; i < jugadores.size(); i++) {
            List<Card> mano = jugadores.get(i).Jugador().getMano();
            int valorMano = mano.stream().mapToInt(Card::getCardNum).sum();
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
            List<Card> mano = jugadores.get(i).Jugador().getMano();
            int valorMano = mano.stream().mapToInt(Card::getCardNum).sum();
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
            List<Card> mano = jugador.Jugador().getMano();
            Map<Integer, Integer> conteoCartas = new HashMap<>();
            for (Card carta : mano) {
                conteoCartas.put(carta.getCardNum(), conteoCartas.getOrDefault(carta.getCardNum(), 0) + 1);
            }
            int jerarquia = 0;
            int valorJerarquia = 0;
            int parejas = 0;
            for (Map.Entry<Integer, Integer> entry : conteoCartas.entrySet()) {
                int valor = entry.getKey();
                int cantidad = entry.getValue();
                if (cantidad == 4) { jerarquia = 4; valorJerarquia = valor; break; }
                else if (cantidad == 3) { if (jerarquia < 3) { jerarquia = 3; valorJerarquia = valor; } }
                else if (cantidad == 2) {
                    parejas++;
                    if (parejas == 2 && jerarquia < 2) { jerarquia = 2; valorJerarquia = Math.max(valorJerarquia, valor); }
                    else if (jerarquia < 1) { jerarquia = 1; valorJerarquia = valor; }
                }
            }
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
            List<Card> mano = jugador.Jugador().getMano();
            int juego = calcularJuego(mano);
            if (juego > mejorJuego) { mejorJuego = juego; posicionGanador = i; }
        }
        return posicionGanador;
    }

    private int calcularJuego(List<Card> mano) {
        Map<Integer, Integer> valoresCartas = Map.of(
                8, 10,
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
        List<Integer> valoresMano = mano.stream()
                .map(Card::getCardNum)
                .map(valoresCartas::get)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        int suma = valoresMano.get(0) + valoresMano.get(1);
        List<Integer> jerarquia = Arrays.asList(31, 32, 40, 39, 38, 37, 36, 35, 34, 33);
        if (jerarquia.contains(suma)) { return jerarquia.indexOf(suma); }
        return -1;
    }

    public int posicionJugadorMasCercaDe30(List<PlayerHandler> jugadores) {
        int posicionMejorCercaDe30 = -1;
        int mejorDiferencia = Integer.MAX_VALUE;
        for (int i = 0; i < jugadores.size(); i++) {
            PlayerHandler jugador = jugadores.get(i);
            List<Card> mano = jugador.Jugador().getMano();
            int juego = calcularJuego(mano);
            if (juego <= 30 && juego >= 0) {
                int diferencia = 30 - juego;
                if (diferencia < mejorDiferencia) { mejorDiferencia = diferencia; posicionMejorCercaDe30 = i; }
            }
        }
        return posicionMejorCercaDe30;
    }

    private void apuestasEquipoContrario(int i,int[] apuestas){
        if(apuestas[(i+1)%4]!=0){
            jugadores.get(i%4).sendMensajeJugador("El jugador "+  jugadores.get((i+1)%4).Jugador().getNombre() + " ha apostado " + apuestas[(i+1)%4]);
        }
        if(apuestas[(i+3)%4]!=0){
            jugadores.get(i%4).sendMensajeJugador("El jugador "+  jugadores.get((i+3)%4).Jugador().getNombre() + " ha apostado " + apuestas[(i+3)%4]);
        }
    }

    private int CantidadapuestasEquipoContrario(int i,int[] apuestas){
        int n=apuestas[(i+1)%4];
        if(apuestas[(i+3)%4]>n){ n=apuestas[(i+3)%4]; }
        return n;
    }

    /**
     * Pregunta a los adversarios en orden si aceptan una apuesta.
     * Primero pregunta al jugador (bettor+1)%4; si pasa, pregunta al otro adversario (bettor+3)%4.
     * Si alguno acepta devuelve true y notifica a todos que aceptó.
     */
    private boolean promptAdversariosAceptarApuesta(int bettorIndex, int amount) {
        int first = (bettorIndex + 1) % 4;
        int second = (bettorIndex + 3) % 4;
        String msg = "El jugador " + jugadores.get(bettorIndex).Jugador().getNombre() + " ha apostado " + amount + ", desea aceptar (A) o pasar (P)?";

        jugadores.get(first).sendMensajeJugador(msg);
        jugadores.get(first).sendMensajeJugador("COD 23");
        String r1 = jugadores.get(first).recibirLineaJugador();
        if (r1 != null && (r1.equalsIgnoreCase("A") || r1.equalsIgnoreCase("Aceptar") || r1.equalsIgnoreCase("aceptar"))) {
            mensajeTodosJugadores("El jugador " + jugadores.get(first).Jugador().getNombre() + " ha aceptado la apuesta de " + amount);
            return true;
        }

        jugadores.get(second).sendMensajeJugador(msg);
        jugadores.get(second).sendMensajeJugador("COD 23");
        String r2 = jugadores.get(second).recibirLineaJugador();
        if (r2 != null && (r2.equalsIgnoreCase("A") || r2.equalsIgnoreCase("Aceptar") || r2.equalsIgnoreCase("aceptar"))) {
            mensajeTodosJugadores("El jugador " + jugadores.get(second).Jugador().getNombre() + " ha aceptado la apuesta de " + amount);
            return true;
        }

        return false;
    }

    private boolean handleFold(int foldingPlayerIndex, int[] apuestas) {
        int team = foldingPlayerIndex % 2;
        int idxA = team;
        int idxB = team + 2;
        int sumFold = 0;
        if (apuestas != null) {
            sumFold += apuestas[idxA];
            sumFold += apuestas[idxB];
        }
        int oppA = (team == 0) ? 1 : 0;
        int oppB = (team == 0) ? 3 : 2;
        if (sumFold > 0) {
            jugadores.get(oppA).Jugador().sumarPuntos(sumFold);
            jugadores.get(oppB).Jugador().sumarPuntos(sumFold);
        }
        mensajeTodosJugadores("El equipo de " + jugadores.get(foldingPlayerIndex).Jugador().getNombre() + " se ha retirado. El equipo contrario gana " + sumFold + " puntos.");
        return true;
    }

    /**
     * Procesa una fase de apuesta (Grandes, Pequenyas, Pares, Juego, Punto) de forma genérica.
     * Devuelve el índice del jugador que hizo ordago (>= puntosNecesarios), -3 si la apuesta fue aceptada por adversarios,
     * -2 si hubo fold resuelto, o -1 si no hubo ordago.
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

            if (msg.toLowerCase().startsWith("apostar") || msg.matches("\\d+")) {
                String montoStr = null;
                String lower = msg.trim();
                String[] tokens = lower.split("\\s+");
                if (tokens.length >= 2 && tokens[0].equalsIgnoreCase("apostar")) {
                    montoStr = tokens[1];
                } else if (tokens.length == 1 && tokens[0].matches("\\d+")) {
                    montoStr = tokens[0];
                } else if (tokens.length == 1 && tokens[0].equalsIgnoreCase("apostar")) {
                    ph.sendMensajeJugador("OK");
                    montoStr = ph.recibirLineaJugador();
                }

                if (montoStr == null || montoStr.isEmpty()) { ph.sendMensajeJugador("ERROR"); continue; }
                int monto;
                try { monto = Integer.parseInt(montoStr.trim()); } catch (NumberFormatException e){ ph.sendMensajeJugador("ERROR"); continue; }

                if (monto >= puntosNecesarios) {
                    // ordago
                    ph.sendMensajeJugador("COD 28");
                    jugadores.get((i % 4 + 1) % 4).sendMensajeJugador("ORDAGO: El jugador " + j.getNombre() + " ha hecho ORDAGO a " + palo + ". Responda 'aceptar ordago' o 'no aceptar' (COD 28)");
                    jugadores.get((i % 4 + 3) % 4).sendMensajeJugador("ORDAGO: El jugador " + j.getNombre() + " ha hecho ORDAGO a " + palo + ". Responda 'aceptar ordago' o 'no aceptar' (COD 28)");
                    return i % 4;
                } else if (monto >= minApuesta) {
                    ph.sendMensajeJugador("COD 19");
                    apuestas[i%4] = monto + CantidadapuestasEquipoContrario(i, apuestas);
                    // Preguntar a adversarios si aceptan esta apuesta
                    boolean acept = promptAdversariosAceptarApuesta(i%4, monto);
                    if (acept) {
                        mensajeTodosJugadores("La apuesta de " + j.getNombre() + " de " + monto + " ha sido aceptada.");
                        return -3; // señal de que la apuesta fue aceptada y hay que avanzar de fase
                    }
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

