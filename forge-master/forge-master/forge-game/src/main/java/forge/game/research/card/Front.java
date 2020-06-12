/**
 * Class that allows for the correct subclass of CardEvaluator to be chosen based on the card's
 * type
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.card;

import forge.game.card.Card;

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
        } else if(card.isEnchantment()) {
            evaluator = new EvaluatorStrategy(new EnchantmentEval());
        } else if(card.isArtifact()) {
            evaluator = new EvaluatorStrategy(new ArtifactEval());
        } else if(card.isLand()) {
            evaluator = new EvaluatorStrategy(new LandEval());
        } else if(card.isPlaneswalker()) {
            evaluator = new EvaluatorStrategy(new PlaneswalkerEval());
        } else if(card.isInstant()) {
            evaluator = new EvaluatorStrategy(new InstantEval());
        } else if(card.isSorcery()) {
            evaluator = new EvaluatorStrategy(new SorceryEval());
        } else { //default - can be changed
            evaluator = new EvaluatorStrategy(new CreatureEval());
        }
        //evaluation
        double value = evaluator.evaluate(card);

        //just to see results
        //System.out.println(card.getName());
        //System.out.println(value);

        //Account this for food and clue tokens
        if (value <= 0) {
            value = 1.5;
        }

        return value;
    }
    
}
