package forge.game.research.decision;

import forge.game.ability.effects.SurveilEffect;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollectionView;

public class TemplateSurveil extends CardTemplate{
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
