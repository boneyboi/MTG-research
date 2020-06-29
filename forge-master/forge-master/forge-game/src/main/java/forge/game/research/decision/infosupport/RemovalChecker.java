package forge.game.research.decision.infosupport;

import forge.game.spellability.SpellAbility;
import forge.game.CardTraitBase;
import forge.game.card.Card;
import forge.game.ability.SpellAbilityEffect;

public class RemovalChecker {

    private SpellAbility sa;

    public RemovalChecker(){
    }

    /**
     * To add:
     * isCurse - sorcery/instants
     * ailogic.equals("Curse") - enchantment
     * hasparam("Destroy" & "DestroyAll")
     * getParam
     */

    public boolean doTargetOthers(SpellAbility spellability) {
        boolean targetOthers = false;
        Card card = spellability.getHostCard();

        sa = spellability;
        String destination = sa.getParam("Destination");

        if (sa.hasParam("Destination") && !(destination.equals("Battlefield"))) {
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("Destroy")) {
            targetOthers = true;
        }
        else if (sa.getHostCard().isEnchantment() && sa.isCurse()) {
            targetOthers = true;
        }
        else {
            System.out.println("?");
        }


        return targetOthers;
    }
}
