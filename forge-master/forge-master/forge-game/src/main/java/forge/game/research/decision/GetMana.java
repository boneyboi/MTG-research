package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
            manaPossible += 1 - c.getManaAbilities().size();
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
