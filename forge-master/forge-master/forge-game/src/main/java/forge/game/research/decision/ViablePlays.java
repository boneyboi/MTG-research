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
        //TODO: Account for sources that provide more than one mana?
        manaPossible = 0;
        for (Card c: controller.getZone(ZoneType.Battlefield))
            if (!c.getManaAbilities().isEmpty() && c.isUntapped()) {
                manaPossible += 1;
            }
        if (controller.getZone(ZoneType.Hand).contains(Card::isLand)
                && controller.getLandsPlayedThisTurn() == 0) {
            manaPossible+=1;
        }
    }

    public void addZoneOptions(ZoneType z) {
        for (Card c: controller.getZone(z)) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= manaPossible) {
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
        //TODO: addLandOptions();
    }

    public void emptyOptions() {
        plays = new ArrayList<SpellAbility>();
    }

}
