package forge.game.research.Card;

import forge.game.card.Card;
import forge.game.zone.ZoneType;
/**
 * (For Research) Description later
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

public class PlaneswalkerEval extends CardEvaluator{



    @Override
    public double evaluate(Card card) {
        double Cardvalue = (getCMCValue(card) + getColorValue(card));
        double value = (Cardvalue) * getRareMultiplier(card);
        if (card.isInZone(ZoneType.Battlefield)) {
            value = value * getStatChange(card);
        }
        return value;
    }

    public double getStatChange(Card card) {
        double temp1 = getCurrentLoyalty(card);
        double temp2 = getBaseLoyalty(card);
    return temp1/temp2;
    }

    public int getCurrentLoyalty(Card card) {
        return card.getCurrentLoyalty();
    }

    public int getBaseLoyalty(Card card) {
        return Integer.valueOf(card.getCurrentState().getBaseLoyalty());
    }
}
