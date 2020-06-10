package forge.game.research;

import forge.game.card.Card;
import forge.game.research.*;
public class Front {

    private Card card;
    private EvaluatorStrategy evaluator;

    public Front(Card card){
        this.card = card;
    }

    /**
     * Chooses a 'strategy' or what evaluator to do use
     * @return value - value of a card
     */
    public double chooser () {

        //choose a strategy based on card 'type'
        if(card.isCreature()){
            evaluator = new EvaluatorStrategy(new CreatureEval());
        } else if(card.isEnchantment()){
            evaluator = new EvaluatorStrategy(new EnchantmentEval());
        } else { //default - can be changed
            evaluator = new EvaluatorStrategy(new CreatureEval());
        }
        //evaluation
        double value = evaluator.evaluate(card);

        //just to see results
        //System.out.println(card.getName());
        //System.out.println(value);

        return value;
    }
    
}
