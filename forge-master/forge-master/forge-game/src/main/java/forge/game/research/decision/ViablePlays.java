package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class ViablePlays {
    ArrayList<SpellAbility> plays = new ArrayList<SpellAbility>();
    int manaPossible = 0;
    Player controller = null;

    public ViablePlays(Player p) {
        controller = p;
    }

    public ArrayList<SpellAbility> getPlays() {
        emptyOptions();
        buildOptions();
        return plays;
    }

    public void getMana() {
        manaPossible = 0;
        for (Card c: controller.getZone(ZoneType.Battlefield))
            if (!c.getManaAbilities().isEmpty()) {
                manaPossible += 1;
            }
        manaPossible = controller.getZone(ZoneType.Battlefield).getNumLandsIn();
        if (controller.getZone(ZoneType.Hand).contains(Card::isLand) && controller.getLandsPlayedThisTurn() == 0) {
            manaPossible+=1;
        }
    }

    public void addZoneOptions(ZoneType z) {
        for (Card c: controller.getZone(z)) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= manaPossible && sa.canPlay()) {
                    plays.add(sa);
                    c.getManaCost().getColorProfile();
                }
            }
        }
    }

    public void buildOptions() {
        getMana();
        addZoneOptions(ZoneType.Hand);
        addZoneOptions(ZoneType.Battlefield);
        addZoneOptions(ZoneType.Graveyard);
        addZoneOptions(ZoneType.Exile);
        //addLandOptions();
    }

    public void emptyOptions() {
        plays = new ArrayList<SpellAbility>();
    }

}
