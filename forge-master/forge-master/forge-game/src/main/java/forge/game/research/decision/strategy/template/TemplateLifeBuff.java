package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.trigger.Trigger;

public class TemplateLifeBuff extends CardTemplate {
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
