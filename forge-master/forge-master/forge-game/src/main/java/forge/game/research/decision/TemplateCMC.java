package forge.game.research.decision;

import forge.game.card.Card;

public class TemplateCMC extends CardTemplate{

    int cmc;

    public TemplateCMC(int cmc) {
        this.cmc = cmc;
    }

    @Override
    public boolean matches(Card card){
        return (card.getCMC() == cmc);
    }
}
