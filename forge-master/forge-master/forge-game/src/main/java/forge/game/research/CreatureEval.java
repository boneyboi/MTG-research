/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

package forge.game.research;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

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
        // If the card is in our hand, lower its value if we need to draw more lands to play it.
        if (card.isInZone(ZoneType.Hand) && card.getController().getLandsAvaliable() < card.getCMC()) {
            double temp1 = card.getController().getLandsAvaliable();
            double temp2 = card.getCMC();
            value = value*temp1/temp2;
        }
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
