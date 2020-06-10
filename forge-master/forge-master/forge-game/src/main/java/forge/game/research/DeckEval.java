package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DeckEval extends ZoneEvaluator{
    public static final double DECKMUL = 1;

    public DeckEval(Player p) {
        super(ZoneType.Library, p, DECKMUL);
    }

    public double evaluateDeck() {
        double numLands = p.getZone(ZoneType.Library).getNumLandsIn();
        double cardNum = p.getZone(ZoneType.Library).size();
        return numLands/cardNum;
    }
}
