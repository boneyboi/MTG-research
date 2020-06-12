/**
 * Subclass of CardEvaluator - determines the value of cards with the Instant 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.Card;

import forge.game.card.Card;

public class InstantEval extends CardEvaluator{

    public double evaluate(Card card) {
        double cardValue = (getCMCValue(card) + getColorValue(card));
        double totalValue = (cardValue) * getRareMultiplier(card);

        return totalValue;
    }
}
