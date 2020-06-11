package forge.game.research.Card;

import forge.game.card.Card;

public class SorceryEval extends CardEvaluator{

    public double evaluate(Card card) {
        double cardValue = (getCMCValue(card) + getColorValue(card));
        double totalValue = (cardValue) * getRareMultiplier(card);
        return totalValue;
    }
}
