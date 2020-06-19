package forge.game.research.decision.strategy.template;

import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollectionView;

public class TemplateLifelink extends CardTemplate {

    public TemplateLifelink() {

    }

    @Override
    public boolean matches(Card card){
        FCollectionView<SpellAbility> stuff = card.getSpellAbilities();
        for (SpellAbility sa: stuff) {
            if (sa.getMapParams().containsValue("GainLife")) {
                return true;
            }
        }
        return ((card.hasKeyword(Keyword.LIFELINK) ||
                card.hasSVar("TrigGainLife") ||
                card.hasSVar("DBGainLife"))
                && !card.isLand());
    }
}
