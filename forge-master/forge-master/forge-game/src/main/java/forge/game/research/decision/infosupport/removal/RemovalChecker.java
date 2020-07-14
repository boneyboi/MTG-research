package forge.game.research.decision.infosupport.removal;

import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.card.Card;

public class RemovalChecker {

    public RemovalChecker(){
    }

    /**
     * Main method, checks to see if a card is removal, should be used to target opponent's creatures
     * @param card
     * @return ture if a card should be used against an opponent, not on our creatures
     */
    public boolean shouldTargetOthers(Card card) {
        boolean shallTargetOthers = false;

        //looks through a card's spell abilities and see if any one of them counts as 'removal'
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (isRemoval(sa)) {
                shallTargetOthers = true;
            }
        }

        return shallTargetOthers;
    }

    /**
     * Checks to see if a spell ability of a card constitutes as removal:
     * exiling a card/all cards
     * destroying a card/all cards
     * prohibits a card from doing certain actions (i.e. Pacifism)
     * debuffs a card (i.e. Dead Weight or Disfigure)
     * @param spellability -
     * @return
     */
    public boolean isRemoval(SpellAbility spellability) {
        boolean targetOthers = false;

        SpellAbility sa = spellability;
        String origin = sa.getParam("Origin");
        String destination = sa.getParam("Destination");

        //checks to see if a card changes the zone of another card, then checks if it will move a card from
        //the battlefield to anywhere else
        if (sa.getApi().name().equals("ChangeZone") && origin.equals("Battlefield") && !(destination.equals("Battlefield"))){
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("Destroy")) {
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("DestroyAll")) {
            targetOthers = true;
        }
        else if (sa.getApi().name().equals("ChangeZone") || sa.getApi().name().equals("ChangeZoneAll")) {
            targetOthers = true;
        }
        //checks to see if the parameter AILogic and IsCurse exists to prevent any null exceptions
        //then gets either parameter if they exist and checks if they contain Curse or True
        else if (sa.hasParam("AILogic") && sa.getParam(
                "AILogic").equals("Curse") || sa.hasParam("IsCurse") && sa.getParam(
                        "IsCurse").equals("True")) {
            targetOthers = true;
        }
        //these are 'choose some number of effects' cards (i.e. Austere Command, Merciless Evicition)
        else if (sa.getApi().name().equals("Charm")) {
            //iterates through a card's spell ability's additional abilities to obtain its effect
         for (AbilitySub ab: sa.getAdditionalAbilityLists().get("Choices")) {
             if (ab.toString().contains("Destroy all") || ab.toString().contains("Exile all")) {
                 targetOthers = true;
             }
         }
        }

        return targetOthers;
    }


}
