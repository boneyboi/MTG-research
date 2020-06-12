/**
 * Subclass of CardEvaluator - determines the value of cards with the Land 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */
package forge.game.research.Card;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class LandEval extends CardEvaluator{

    public LandEval(){
        super();
    }

    public final double evaluate (Card card) {
        return getRareMultiplier(card);
    }

}
