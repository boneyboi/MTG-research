/**
 * (For Research) Description later
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */
package forge.game.research;


import forge.game.card.Card;

public abstract class CardEvaluator {
    /**
     * evaluate the value of a card
     @param card: Card object
     @return: value of a card as a double
     */
    public abstract double evaluate(Card card);

}