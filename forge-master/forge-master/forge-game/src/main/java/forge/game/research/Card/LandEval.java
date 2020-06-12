/**
 * (For Research) Description later
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */
package forge.game.research.Card;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class LandEval extends CardEvaluator{
    public final double evaluate (Card card) {
        return getRareMultiplier(card);
    }

}
