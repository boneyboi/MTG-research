/**
 * Subclass of CardEvaluator - determines the value of cards with the Land 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 23, 2020
 */
package forge.game.research.card;

import forge.game.card.Card;

public class LandEval extends CardEvaluator{

    public LandEval(){
        super();
    }

    /**
     * For lands, its value relies on its RareMultiplier
     * @param card - card object
     * @return rare multiplier of the land
     */
    @Override
    public double evaluate (Card card) {
        return getRareMultiplier(card);
    }

}
