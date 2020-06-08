/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

package forge.game.research;

import forge.game.card.Card;

public class CreatureEval extends CardEvaluator {

    //constants
    public static final double BASE = .5;

    public CreatureEval() {
    }

    /**
     * Description
     * @param card
     * @return value of card
     */
    public final double evaluate (Card card) {
        double value = (BASE + getStatChange(card) + getCMCValue(card) + getColorValue(card));
        return (value) * getRareMultiplier(card);
    }

}
