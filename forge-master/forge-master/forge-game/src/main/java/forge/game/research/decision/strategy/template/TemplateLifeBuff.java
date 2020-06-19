/**
 * Template to allow for filtering cards based on if a card is able to be buffed in someway when gaining life
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

public class TemplateLifeBuff extends CardTemplate {
    public TemplateLifeBuff() {

    }

    @Override
    public boolean matches(SpellAbility sa){
        Card card = sa.getHostCard();
        for (Trigger t: card.getTriggers()) {
            if (t.getMode().name().equals("LifeGained")) {
                return true;
            }
        }
        return false;
    }
}
