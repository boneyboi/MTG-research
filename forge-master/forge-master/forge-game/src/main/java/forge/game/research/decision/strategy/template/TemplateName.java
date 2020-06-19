/**
 * Description
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;

public class TemplateName extends CardTemplate {
    String name;

    public TemplateName(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(Card card){
        return card.getName().equals(name);
    }
}
