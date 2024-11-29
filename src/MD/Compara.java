package MD;

import java.util.Comparator;

public class Compara implements Comparator<Card> {
    @Override
    public int compare(Card o1, Card o2) {
        return o1.compareToReverse(o2);
    }
}