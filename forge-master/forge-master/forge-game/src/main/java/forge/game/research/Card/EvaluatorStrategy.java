/**
 * Class that allows for an evaluator to be chosen at runtime, leading to a simpler
 * 'choose' mechanism
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.Card;
import forge.game.card.*;
import forge.game.research.Card.CardEvaluator;

public class EvaluatorStrategy {
    private CardEvaluator evaluator;

    //allow an evaluator to be chosen at runtime via the strategy design pattern
    public EvaluatorStrategy(CardEvaluator evaluator){
        this.evaluator = evaluator;
    }

    /**
    allow the specific strategy chosen to evaluate the value of a card
     @param card: Card object
     @return: value of a card as a double
     */
    public double evaluate(Card card){
        return evaluator.evaluate(card);
    }
}
