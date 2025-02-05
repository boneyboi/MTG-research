/**
 * Template to allow for filtering cards based on if a card allows a player to 'surveil'
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.util.collect.FCollectionView;

public class TemplateSurveil extends CardTemplate {
    public TemplateSurveil() {

    }

    @Override
    public boolean matches(SpellAbility spell){
        Card card = spell.getHostCard();
       FCollectionView<SpellAbility> stuff = card.getSpellAbilities();
       for (SpellAbility sa: stuff) {
           if (sa.getMapParams().containsValue("Surveil")) {
               return true;
           }
       }
       if (spell instanceof SpellPermanent) {
           return (card.hasSVar("TrigSurveil") || card.hasSVar("DBSurveil"));
       }
       return false;
    }
}
