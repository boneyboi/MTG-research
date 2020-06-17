package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.util.collect.FCollectionView;

public class TemplateLifeBuff extends CardTemplate{
    public TemplateLifeBuff() {

    }

    @Override
    public boolean matches(Card card){
        for (Trigger t: card.getTriggers()) {
            if (t.getMode().name().equals("LifeGained")) {
                return true;
            }
        }
        return false;
    }
}
