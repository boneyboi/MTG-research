package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.trigger.Trigger;

public class TemplateSurveilBoost extends CardTemplate {
    public TemplateSurveilBoost() {

    }

    @Override
    public boolean matches(Card card){
        for (Trigger t: card.getTriggers()) {
            if (t.getMode().name().equals("Surveil")) {
                return true;
            }
        }
        return false;
    }
}
