/**
 * Subclass of CardEvaluator - determines the value of cards with the Enchantment 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */
package forge.game.research.Card;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.research.Card.CardEvaluator;

public class EnchantmentEval extends CardEvaluator {
    public static final double MYTHICMULTIPLIER = 1.5;

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
    public final double evaluate(Card card) {
        double cardValue = (getCMCValue(card) + getColorValue(card));
        double totalValue = (cardValue) * getRareMultiplier(card);
        return totalValue;
    }

}