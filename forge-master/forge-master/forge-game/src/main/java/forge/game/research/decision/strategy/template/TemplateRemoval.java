/**
 * Template to allow for filtering cards based on CMC
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.ability.SpellApiBased;
import forge.game.card.Card;
import forge.game.research.decision.infosupport.removal.RemovalChecker;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;

public class TemplateRemoval extends CardTemplate {

    public TemplateRemoval() {

    }


    /**
     * Return if the sa is playing the card from hand, and if so, if it is the right cmc
     * @param sa
     * @return
     */
    @Override
    public boolean matches(SpellAbility sa){
        RemovalChecker rc = new RemovalChecker();
        return rc.isRemoval(sa);
    }
}
