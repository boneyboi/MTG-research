/**
 * Subclass of CardEvaluator - determines the value of cards with the Planeswalker 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 23, 2020
 */

package forge.game.research.card;

import forge.game.card.Card;
import forge.game.zone.ZoneType;


public class PlaneswalkerEval extends CardEvaluator{

    //calls card evaluator constructor
    public PlaneswalkerEval() {
        super();
    }


    /**
     * A planeswaler is valuated based on:
     * rarity
     * cmc
     * color
     * stat change (loyalty)
     * @param card - card object
     * @return total valye of the card
     */
    @Override
    public double evaluate(Card card) {
        double Cardvalue = (getCMCValue(card) + getColorValue(card));
        double value = (Cardvalue) * getRareMultiplier(card);

        //accounts for any change in stats while on the battlefield, and not in hand
        if (card.isInZone(ZoneType.Battlefield)) {
            value = value * getStatChange(card);
        }
        return value;
    }

    /**
     * Calculates stat change of planeswalker, instead of power and toughness, this uses loyalty
     * @param card
     * @return loyalty change
     */
    public double getStatChange(Card card) {
        double temp1 = getCurrentLoyalty(card);
        double temp2 = getBaseLoyalty(card);
    return temp1/temp2;
    }

    /**
     * Helper method that returns the current loyalty of the planeswalker
     * @param card
     * @return current loyalty of planeswalker
     */
    private int getCurrentLoyalty(Card card) {
        return card.getCurrentLoyalty();
    }

    /**
     * Helper method that returns the base/inital loyalty of the planeswalker
     * @param card
     * @return base loyalty of planeswalker
     */
    private int getBaseLoyalty(Card card) {
        return Integer.valueOf(card.getCurrentState().getBaseLoyalty());
    }
}
