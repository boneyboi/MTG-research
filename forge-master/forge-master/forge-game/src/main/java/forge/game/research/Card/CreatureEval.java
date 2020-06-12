/**
 * Subclass of CardEvaluator - determines the value of cards with the Creature 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.Card;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.research.Card.CardEvaluator;

public class CreatureEval extends CardEvaluator {


    public static final double BASE = .5;
    public static final int STATBOOST2 = 2;
    public static final int STATBOOST3 = 3;
    public static final int STATBOOST4 = 4;

    public CreatureEval() {
        super();
    }



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

    @Override
    public final double evaluate (Card card) {
        double stats = getStatChange(card);
        double cMC = getCMCValue(card);
        double colors = getColorValue(card);
        double counters = getCounters(card);
        double Cardvalue = (BASE + stats + cMC + colors + counters);
        double value = (Cardvalue) * getRareMultiplier(card);
        return value;
    }

    public double getStatChange (Card card){
        return this.getPowerChange(card) +
               this.getToughnessChange(card);
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
        return  card.getCounters(CounterType.P1P1)*STATBOOST2 +
                card.getCounters(CounterType.P1P0) +
                card.getCounters(CounterType.P0P1) +
                card.getCounters(CounterType.P2P2)*STATBOOST4 +
                card.getCounters(CounterType.P1P2)*STATBOOST3 +
                card.getCounters(CounterType.P0P2)*STATBOOST2 +
                card.getCounters(CounterType.P2P0)*STATBOOST2 -
                card.getCounters(CounterType.M1M1)*STATBOOST2 +
                card.getCounters(CounterType.M1M0) -
                card.getCounters(CounterType.M0M1) -
                card.getCounters(CounterType.M0M2)*STATBOOST2 -
                card.getCounters(CounterType.M2M2)*STATBOOST4 -
                card.getCounters(CounterType.M2M1)*STATBOOST3;
    }
}
