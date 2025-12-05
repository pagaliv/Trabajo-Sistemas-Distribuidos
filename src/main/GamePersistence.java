package main;

import MD.Card;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.Instant;
import java.util.List;

/**
 * Helper to persist a lightweight snapshot of the game using DOM (XML).
 *
 * This writes: metadata (timestamp, current index) and a list of players with name, score and their current hand.
 */
public class GamePersistence {

    /**
     * Save a snapshot to the given file path.
     * @param juego game instance (must be in the same package so we can use its getters)
     * @param filePath path to write XML to
     * @throws Exception on IO / parser errors
     */
    public static void saveSnapshot(Juego juego, String filePath) throws Exception {
        saveSnapshot(juego, new File(filePath));
    }

    public static void saveSnapshot(Juego juego, File file) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element rootElement = doc.createElement("GameSnapshot");
        rootElement.setAttribute("timestamp", Instant.now().toString());
        rootElement.setAttribute("currentPlayerIndex", String.valueOf(juego.getIndiceJugadorActual()));
        doc.appendChild(rootElement);

        Element playersEl = doc.createElement("Players");
        rootElement.appendChild(playersEl);

        List<PlayerHandler> players = juego.getJugadores();
        for (int i = 0; i < players.size(); i++) {
            PlayerHandler ph = players.get(i);
            Jugador j = ph.Jugador();

            Element pEl = doc.createElement("Player");
            pEl.setAttribute("index", String.valueOf(i));
            pEl.setAttribute("name", j.getNombre());
            pEl.setAttribute("score", String.valueOf(j.getPuntuacion()));
            pEl.setAttribute("active", String.valueOf(j.isActivo()));

            Element handEl = doc.createElement("Hand");
            for (Card c : j.getMano()) {
                Element cardEl = doc.createElement("Card");
                cardEl.setAttribute("value", String.valueOf(c.getCardNum()));
                cardEl.setAttribute("suit", String.valueOf(c.getSuit()));
                handEl.appendChild(cardEl);
            }
            pEl.appendChild(handEl);

            playersEl.appendChild(pEl);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }
}
