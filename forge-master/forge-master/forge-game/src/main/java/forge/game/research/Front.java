package forge.game.research;

import forge.game.card.Card;
import forge.game.research.*;
public class Front {
    public Front(Card card){
        //choose a strategy
        EvaluatorStrategy evaluator;
        if(card.isCreature()){
            evaluator = new EvaluatorStrategy(new CreatureEval());
        } else if(card.isEnchantment()){
            evaluator = new EvaluatorStrategy(new EnchantmentEval());
        } else { //default - can be changed
            evaluator = new EvaluatorStrategy(new CreatureEval());
        }
        System.out.println(evaluator.evaluate(card));
    }
    
}
