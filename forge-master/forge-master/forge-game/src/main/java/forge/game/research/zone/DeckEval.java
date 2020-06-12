/**
 * Subclass for ZoneEvaluator -- gives a value to the zone 'library'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.zone;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DeckEval extends ZoneEvaluator {

    public DeckEval(Player p) {
        super(ZoneType.Library, p);
    }

    /**
     * Evaluates the value of the deck: the number of lands divided by the size
     * @return double average: number of lands/number of cards in the deck
     */
    @Override
    public double evaluateZone() {
        double numLands = p.getZone(ZoneType.Library).getNumLandsIn();
        double cardNum = p.getZone(ZoneType.Library).size();
        return numLands/cardNum;
    }
}
