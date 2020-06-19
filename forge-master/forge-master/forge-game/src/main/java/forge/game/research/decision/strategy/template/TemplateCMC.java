/**
 * Template to allow for filtering cards based on CMC
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TemplateCMC extends CardTemplate {

    int cmc;

    public TemplateCMC(int cmc) {
        this.cmc = cmc;
    }

    @Override
    public boolean matches(SpellAbility sa){
        return (sa.getPayCosts().getTotalMana().getCMC() == cmc);
    }
}
