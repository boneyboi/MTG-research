package forge.game.research.Card;

import forge.card.CardType;
import forge.card.CardTypeView;
import forge.game.card.Card;
import forge.game.research.Card.CreatureEval;
import forge.game.research.Card.EnchantmentEval;
import forge.game.research.Card.EvaluatorStrategy;

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
        if (card.isToken()) {
            evaluator = new EvaluatorStrategy(new TokenEval());
        } else if(card.isCreature()){
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

        return value;
    }
    
}
