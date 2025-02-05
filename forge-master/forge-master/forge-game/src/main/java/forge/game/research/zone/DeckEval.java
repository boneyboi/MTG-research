/**
 * Subclass for ZoneEvaluator -- gives a value to the zone 'library'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.zone;

import forge.game.card.Card;
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

    public double averageManaCost(){
        double sum = 0;
        int numofnonlands = 0;
        for(Card c : p.getCardsIn(ZoneType.Library)){
            if(!c.isLand()){sum+=c.getManaCost().getCMC(); numofnonlands++;}
        }
        for(Card c : p.getCardsIn(ZoneType.Hand)){
            if(!c.isLand()){sum+=c.getManaCost().getCMC(); numofnonlands++;}
        }
        return sum/numofnonlands;
    }

}
