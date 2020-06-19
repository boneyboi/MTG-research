/**
 * Template to allow for filtering cards based on if a card is buffed if the player 'surveils'
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

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
