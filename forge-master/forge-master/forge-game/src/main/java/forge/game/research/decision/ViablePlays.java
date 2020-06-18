package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class ViablePlays {
    ArrayList<SpellAbility> plays = new ArrayList<SpellAbility>();
    int manaPossible = 0;
    Player controller = null;
    int mountainNum = 0;
    int swampNum = 0;
    int islandNum = 0;
    int forestNum = 0;
    int plainsNum = 0;

    boolean plainsPos;
    boolean forestPos;
    boolean islandPos;
    boolean swampPos;
    boolean mountainPos;
    boolean manaPos;


    public ViablePlays(Player p) {
        controller = p;
    }

    public void checkPossibleColorPlays() {
        mountainPos = false;
        swampPos = false;
        forestPos = false;
        plainsPos = false;
        islandPos = false;
        manaPos = false;


        if (controller.getZone(ZoneType.Hand).contains(Card::isLand)
                && controller.getLandsPlayedThisTurn() == 0) {
            for (Card c: controller.getZone(ZoneType.Hand)) {
                if (c.isLand()
                        && !c.hasKeyword("ETBReplacement:Other:LandTapped")
                        && !c.hasKeyword("CARDNAME enters the battlefield tapped.")
                        && !c.hasKeyword("ETBReplacement:Other:DBTap")) {
                    for (SpellAbility sa : c.getManaAbilities()) {
                        String type = sa.getMapParams().get("Produced");
                        if (type.contains("R")) {
                            mountainPos = true;
                            manaPos = true;
                        }
                        if (type.contains("B")) {
                            swampPos = true;
                            manaPos = true;
                        }
                        if (type.contains("U")) {
                            islandPos = true;
                            manaPos = true;
                        }
                        if (type.contains("G")) {
                            forestPos = true;
                            manaPos = true;
                        }
                        if (type.contains("W")) {
                            plainsPos = true;
                            manaPos = true;
                        }

                    }
                }
            }
            if (manaPos) {
                manaPossible += 1;
            }
        }
    }

    public ArrayList<SpellAbility> getPlays() {
        emptyOptions();
        buildOptions();
        return plays;
    }


    public void getMana() {
        manaPossible = 0;
        mountainNum = 0;
        swampNum = 0;
        islandNum = 0;
        forestNum = 0;
        plainsNum = 0;
        for (Card c: controller.getZone(ZoneType.Battlefield)) {
            for (SpellAbility sa : c.getManaAbilities()) {
                String type = sa.getMapParams().get("Produced");
                if (type.contains("R")) {
                        mountainNum += 1;
                        manaPossible += 1;
                    }
                if (type.contains("B")) {
                        swampNum += 1;
                    manaPossible += 1;
                    }
                if (type.contains("U")) {
                        islandNum += 1;
                    manaPossible += 1;
                    }
                if (type.contains("G")) {
                        forestNum += 1;
                    manaPossible += 1;
                    }
                if (type.contains("W")) {
                        plainsNum += 1;
                    manaPossible += 1;
                    }

            }
            manaPossible += 1 - c.getManaAbilities().size();
        }

    }

    public void addZoneOptions(ZoneType z) {
        for (Card c: controller.getZone(z)) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= manaPossible
                        && sa.canPlay()) {
                    plays.add(sa);
                }
            }
        }
    }

    public void buildOptions() {
        getMana();
        checkPossibleColorPlays();
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
