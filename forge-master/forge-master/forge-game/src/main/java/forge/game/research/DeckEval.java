package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DeckEval extends ZoneEvaluator{
    public static final double DECKMUL = 1;

    public DeckEval(Player p) {
        super(ZoneType.Library, p, DECKMUL);
    }

    /**
     * Evaluates the value of the deck: the number of lands divided by the size
     * @return double average: number of lands/number of cards in the deck
     */
    public double evaluateDeck() {
        double numLands = p.getZone(ZoneType.Library).getNumLandsIn();
        double cardNum = p.getZone(ZoneType.Library).size();
        return numLands/cardNum;
    }
}
