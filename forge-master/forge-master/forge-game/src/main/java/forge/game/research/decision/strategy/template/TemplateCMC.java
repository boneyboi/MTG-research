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


    /**
     * Return if the sa is playing the card from hand, and if so, if it is the right cmc
     * @param sa
     * @return
     */
    @Override
    public boolean matches(SpellAbility sa){
        if (sa.equals(sa.getHostCard().getFirstSpellAbility())){
            return (sa.getPayCosts().getTotalMana().getCMC() == cmc);
        }
        return false;
    }
}
