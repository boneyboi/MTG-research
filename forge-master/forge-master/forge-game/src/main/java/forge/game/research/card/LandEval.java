/**
 * Subclass of CardEvaluator - determines the value of cards with the Land 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */
package forge.game.research.card;

import forge.game.card.Card;

public class LandEval extends CardEvaluator{

    public LandEval(){
        super();
    }

    @Override
    public double evaluate (Card card) {
        return getRareMultiplier(card);
    }

}