/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

package forge.game.research.Card;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.research.Card.CardEvaluator;

public class CreatureEval extends CardEvaluator {


    public static final double BASE = .5;


    /**
     * A card's value depends on its:
     *  rarity
     *  number of colors
     *  number of colored mana symbols in its cost
     *  CMC
     *  base value (.5 to account for 0-cost creatures)
     * @param card - card object
     * @return the value of the card
     */

    public final double evaluate (Card card) {
        double Cardvalue = (BASE + getStatChange(card) + getCMCValue(card) + getColorValue(card))
                +getCounters(card);
        double value = (Cardvalue) * getRareMultiplier(card);

        return value;
    }

    public double getStatChange (Card card){
        return this.getPowerChange(card)
                + this.getToughnessChange(card);
    }

    public double getPowerChange (Card card){
        if (card.getCopiedPermanent() != null) {
            return card.getCurrentPower() - card.getPaperCard().getRules().getMainPart().getIntPower();
        } else {
            return card.getCurrentPower() - card.getBasePower();
        }
    }


    public double getToughnessChange (Card card){
        if (card.getCopiedPermanent() != null) {
            return card.getCurrentToughness() - card.getPaperCard().getRules().getMainPart().getIntToughness();
        } else {
            return card.getCurrentToughness() - card.getBaseToughness();
        }
    }

    public double getCounters (Card card) {
        return card.getCounters(CounterType.P1P1) + card.getCounters(CounterType.P1P0) +
                card.getCounters(CounterType.P0P1) + card.getCounters(CounterType.P2P2)
                + card.getCounters(CounterType.P1P2) + card.getCounters(CounterType.P0P2)
                + card.getCounters(CounterType.P2P0)
                - card.getCounters(CounterType.M1M1) - card.getCounters(CounterType.M1M0)
                - card.getCounters(CounterType.M0M1) - card.getCounters(CounterType.M0M2)
                - card.getCounters(CounterType.M2M2) - card.getCounters(CounterType.M2M1);
    }
}
