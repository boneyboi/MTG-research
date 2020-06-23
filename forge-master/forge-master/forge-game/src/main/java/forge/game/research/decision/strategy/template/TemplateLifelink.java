/**
 * Template to allow for filtering cards based on if a card allows the player to gain life
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.util.collect.FCollectionView;

public class TemplateLifelink extends CardTemplate {

    public TemplateLifelink() {

    }

    @Override
    public boolean matches(SpellAbility spell){
        Card card = spell.getHostCard();
        FCollectionView<SpellAbility> stuff = card.getSpellAbilities();
        for (SpellAbility sa: stuff) {
            if (sa.getMapParams().containsValue("GainLife")) {
                return true;
            }
        }
        if (spell instanceof SpellPermanent || card.isInstant() || card.isSorcery()) {
            return ((card.hasKeyword(Keyword.LIFELINK) ||
                    card.hasSVar("TrigGainLife") ||
                    card.hasSVar("DBGainLife"))
                    && !card.isLand());
        }
        return false;
    }
}
