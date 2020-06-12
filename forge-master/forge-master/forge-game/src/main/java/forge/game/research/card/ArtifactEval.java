/**
 * Subclass of CardEvaluator - determines the value of cards with the Artifact 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.card;

import forge.game.card.Card;

public class ArtifactEval extends CardEvaluator{

    public ArtifactEval() {
    }

    public double evaluate(Card card) {
        double cardValue = (getCMCValue(card) + getColorValue(card));
        double totalValue = (cardValue) * getRareMultiplier(card);

        return totalValue;
    }
}
