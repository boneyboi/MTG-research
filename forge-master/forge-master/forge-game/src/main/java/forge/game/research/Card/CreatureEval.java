/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

package forge.game.research.Card;

import forge.game.card.Card;
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
    @Override
    public final double evaluate (Card card) {
        double Cardvalue = (BASE + getStatChange(card) + getCMCValue(card) + getColorValue(card));
        double value = (Cardvalue) * getRareMultiplier(card);

        return value;
    }

    public double getStatChange (Card card){
        return this.getPowerChange(card)
                + this.getToughnessChange(card);
    }

    public double getPowerChange (Card card){
        return card.getCurrentPower() - card.getBasePower();
    }


    public double getToughnessChange (Card card){
        return card.getCurrentToughness() - card.getBaseToughness();
    }
}
