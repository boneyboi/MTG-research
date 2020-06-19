/**
 * Template to allow for filtering cards based on the name of a card
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TemplateName extends CardTemplate {
    String name;

    public TemplateName(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(SpellAbility sa){
        return sa.getHostCard().getName().equals(name);
    }
}
