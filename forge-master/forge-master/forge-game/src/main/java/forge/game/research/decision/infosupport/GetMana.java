package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class GetMana{
    public static final String RED = "R";
    public static final String GREEN = "G";
    public static final String BLUE = "U";
    public static final String BLACK = "B";
    public static final String WHITE = "W";

    int manaPossible = 0;

    int mountainNum = 0;
    int swampNum = 0;
    int islandNum = 0;
    int forestNum = 0;
    int plainsNum = 0;

    boolean plainsAvaliable;
    boolean forestAvaliable;
    boolean islandAvaliable;
    boolean swampAvaliable;
    boolean mountainAvaliable;
    boolean manaAvaliable;

    Player controller;

    public GetMana(Player p) {
        controller = p;
        getMana();
        checkPossibleColorPlays();
    }

    public ArrayList<Integer> getReturnValues() {
        ArrayList<Integer> returns = new ArrayList<Integer>();
        returns.add(manaPossible);
        returns.add(plainsNum);
        returns.add(islandNum);
        returns.add(swampNum);
        returns.add(mountainNum);

        appendBooleanValue(manaAvaliable, returns);
        appendBooleanValue(plainsAvaliable, returns);
        appendBooleanValue(islandAvaliable, returns);
        appendBooleanValue(swampAvaliable, returns);
        appendBooleanValue(mountainAvaliable, returns);
        appendBooleanValue(forestAvaliable, returns);
        return returns;
    }

    private void appendBooleanValue(Boolean b, ArrayList<Integer> i) {
        if (b) {
            i.add(0);
        } else {
            i.add(1);
        }
    }

    public void getMana() {
        manaPossible = 0;
        mountainNum = 0;
        swampNum = 0;
        islandNum = 0;
        forestNum = 0;
        plainsNum = 0;
        for (Card c: controller.getZone(ZoneType.Battlefield)) {
            if (!c.isTapped()) {
                for (SpellAbility sa : c.getManaAbilities()) {
                    String type = sa.getMapParams().get("Produced");
                    if (type.contains(RED)) {
                        mountainNum += 1;
                        manaPossible += 1;
                    }
                    if (type.contains(BLACK)) {
                        swampNum += 1;
                        manaPossible += 1;
                    }
                    if (type.contains(BLUE)) {
                        islandNum += 1;
                        manaPossible += 1;
                    }
                    if (type.contains(GREEN)) {
                        forestNum += 1;
                        manaPossible += 1;
                    }
                    if (type.contains(WHITE)) {
                        plainsNum += 1;
                        manaPossible += 1;
                    }

                }
                if (!c.getManaAbilities().isEmpty()) {
                    manaPossible += 1 - c.getManaAbilities().size();
                }
            }
        }

    }

    public void checkPossibleColorPlays() {
        mountainAvaliable = false;
        swampAvaliable = false;
        forestAvaliable = false;
        plainsAvaliable = false;
        islandAvaliable = false;
        manaAvaliable = false;


        if (controller.getZone(ZoneType.Hand).contains(Card::isLand)
                && controller.getLandsPlayedThisTurn() == 0) {
            for (Card c: controller.getZone(ZoneType.Hand)) {
                if (c.isLand()
                        && !c.hasKeyword("ETBReplacement:Other:LandTapped")
                        && !c.hasKeyword("CARDNAME enters the battlefield tapped.")
                        && !c.hasKeyword("ETBReplacement:Other:DBTap")) {
                    for (SpellAbility sa : c.getManaAbilities()) {
                        String type = sa.getMapParams().get("Produced");
                        if (type.contains(RED)) {
                            mountainAvaliable = true;
                            manaAvaliable = true;
                        }
                        if (type.contains(BLACK)) {
                            swampAvaliable = true;
                            manaAvaliable = true;
                        }
                        if (type.contains(BLUE)) {
                            islandAvaliable = true;
                            manaAvaliable = true;
                        }
                        if (type.contains(GREEN)) {
                            forestAvaliable = true;
                            manaAvaliable = true;
                        }
                        if (type.contains(WHITE)) {
                            plainsAvaliable = true;
                            manaAvaliable = true;
                        }

                    }
                }
            }
            if (manaAvaliable) {
                manaPossible += 1;
            }
        }
    }
}
