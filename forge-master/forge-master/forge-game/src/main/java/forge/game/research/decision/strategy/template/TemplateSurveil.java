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
import forge.util.collect.FCollectionView;

public class TemplateSurveil extends CardTemplate {
    public TemplateSurveil() {

    }

    @Override
    public boolean matches(Card card){
       FCollectionView<SpellAbility> stuff = card.getSpellAbilities();
       for (SpellAbility sa: stuff) {
           if (sa.getMapParams().containsValue("Surveil")) {
               return true;
           }
       }
        return (card.hasSVar("TrigSurveil") || card.hasSVar("DBSurveil") );
    }
}
