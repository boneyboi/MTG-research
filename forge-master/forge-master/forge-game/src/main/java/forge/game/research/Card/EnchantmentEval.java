package forge.game.research.Card; /**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 09, 2020
 */
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.research.Card.CardEvaluator;

public class EnchantmentEval extends CardEvaluator {


    /**
     * A card's value depends on its:
     *  rarity
     *  number of colors
     *  number of colored mana symbols in its cost
     *  CMC
     * @param card - card object
     * @return the value of the card
     */
    public final double evaluate(Card card) {
        double cardValue = (getCMCValue(card) + getColorValue(card));
        double totalValue = (cardValue) * getRareMultiplier(card);
        return totalValue;
    }

}