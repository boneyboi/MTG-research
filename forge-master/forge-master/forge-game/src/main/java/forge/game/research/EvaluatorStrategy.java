package forge.game.research;
import forge.game.card.*;

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
