package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class ViablePlays {
    ArrayList<SpellAbility> plays = new ArrayList<SpellAbility>();
    Player controller = null;
    ArrayList<Integer> manapool;



    //TODO: add public final string constants for land string literals like "R" or "B"
    public ViablePlays(Player p) {
        controller = p;
    }



    public ArrayList<SpellAbility> getPlays() {
        emptyOptions();
        buildOptions();
        return plays;
    }


    public void addZoneOptions(ZoneType z) {
        for (Card c: controller.getZone(z)) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= (int) manapool.get(0)
                        && sa.canPlay()) {
                    plays.add(sa);
                }
            }
        }
    }

    public void buildOptions() {
        GetMana manaOptions = new GetMana(controller);
        manapool = manaOptions.getReturnValues();
        addZoneOptions(ZoneType.Hand);
        addZoneOptions(ZoneType.Battlefield);
        addZoneOptions(ZoneType.Graveyard);
        addZoneOptions(ZoneType.Exile);
        //TODO: addLandOptions();
    }

    public void emptyOptions() {
        plays = new ArrayList<SpellAbility>();
    }

}
