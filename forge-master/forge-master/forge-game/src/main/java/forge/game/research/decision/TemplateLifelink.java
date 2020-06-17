package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.keyword.Keyword;

public class TemplateLifelink extends CardTemplate{

    public TemplateLifelink() {

    }

    @Override
    public boolean matches(Card card){
        return (card.hasKeyword(Keyword.LIFELINK) || card.hasSVar("TrigGainLife") || card.hasSVar("DBGainLife"));
    }
}
