package forge.game.research.decision.infosupport;

import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.CardTraitBase;
import forge.game.card.Card;
import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbilityVariables;

public class RemovalChecker {

    public RemovalChecker(){
    }

    /**
     * To add:
     * isCurse - sorcery/instants
     * ailogic.equals("Curse") - enchantment
     * hasparam("Destroy" & "DestroyAll")
     * getParam
     */
    public boolean isTargetOthers(Card card) {
        boolean shallTargetOthers = false;

        for (SpellAbility sa : card.getSpellAbilities()) {
            if (doTargetOthers(sa)) {
                shallTargetOthers = true;
            }
        }

        return shallTargetOthers;
    }


    public boolean doTargetOthers(SpellAbility spellability) {
        boolean targetOthers = false;

        SpellAbility sa = spellability;
        String destination = sa.getParam("Destination");

        if (sa.hasParam("Destination") && !(destination.equals("Battlefield"))) {
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("Destroy")) {
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("DestroyAll")) {
            targetOthers = true;
        }
        else if (sa.getHostCard().isEnchantment() && sa.isCurse()) {
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("Charm")) {
         for (String svar : sa.getSVars()) {
             if (svar.contains("Destroy")) {
                 targetOthers = true;
             }
         }
        }
        else {
            System.out.println("?");
        }


        return targetOthers;
    }


}
