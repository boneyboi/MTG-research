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
import forge.game.spellability.SpellPermanent;

public class TemplatePermanentCMC extends CardTemplate {

    private int cmc;
    //TODO: Should this template include playing nonpermanent spells?
    //TODO: Make a new template for abilities?
    public TemplatePermanentCMC(int cmc) {
        this.cmc = cmc;
    }


    /**
     * Return if the sa is playing the card from hand, and if so, if it is the right cmc
     * @param sa
     * @return
     */
    @Override
    public boolean matches(SpellAbility sa){
        if (sa instanceof SpellPermanent){
            return (sa.getPayCosts().getTotalMana().getCMC() == this.cmc);
        }
        return false;
    }

    public int getCMC(){
        return this.cmc;
    }
    public void setCMC(int cmc){
        this.cmc = cmc;
    }

}
