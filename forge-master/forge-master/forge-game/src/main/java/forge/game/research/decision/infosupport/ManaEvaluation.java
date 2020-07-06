/**
 * Calculates how much mana is available and, possibly in the future, the potential mana we could have available a turn
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 23 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class ManaEvaluation {
    public static final String RED = "R";
    public static final String GREEN = "G";
    public static final String BLUE = "U";
    public static final String BLACK = "B";
    public static final String WHITE = "W";

    int manaPool = 0;

    int mountainNum = 0;
    int swampNum = 0;
    int islandNum = 0;
    int forestNum = 0;
    int plainsNum = 0;

    boolean plainsAvaliable = false;
    boolean forestAvaliable = false;
    boolean islandAvaliable = false;
    boolean swampAvaliable = false;
    boolean mountainAvaliable = false;
    boolean manaAvaliable = false;

    public Player controller;

    /**
     * Evaluates the mana pool for a player
     * @param p - player
     */
    public ManaEvaluation(Player p) {
        controller = p;
        getMana();
    }

    public ArrayList<Integer> getManaFromHand() {
        getMana(ZoneType.Hand);
        return getManaCurrent();
    }

    public ArrayList<Integer> getManaPossible() {
        checkPossibleColorPlays();
        return getManaCurrent();
    }

    public ArrayList<Integer> getManaRemaining(ArrayList<SpellAbility> plays) {
        if (plainsAvaliable) {
            plainsNum += 1;
        }
        if (islandAvaliable) {
            islandNum += 1;
        }
        if (swampAvaliable) {
            swampNum += 1;
        }
        if (mountainAvaliable) {
            mountainNum += 1;
        }
        if (forestAvaliable) {
            forestNum += 1;
        }
        for (SpellAbility sa: plays) {
            for (ManaCostShard shard: sa.getPayCosts().getCostMana().getMana()) {
                if (shard.isWhite()) {
                    plainsNum--;
                    manaPool--;
                } else if(shard.isBlue()) {
                    islandNum--;
                    manaPool--;
                } else if (shard.isBlack()){
                    swampNum--;
                    manaPool--;
                } else if (shard.isRed()){
                    mountainNum--;
                    manaPool--;
                } else if (shard.isGreen()) {
                    forestNum--;
                    manaPool--;
                }
            }
            manaPool -= sa.getPayCosts().getCostMana().getMana().getGenericCost();
        }
        ArrayList<Integer> returns = new ArrayList<>();
        returns.add(manaPool);
        returns.add(plainsNum);
        returns.add(islandNum);
        returns.add(swampNum);
        returns.add(mountainNum);
        returns.add(forestNum);

        return returns;
    }




    /**
     * Puts all mana pool values to be returned in a list and returns them
     * @return Arraylist of Integers
     */
    //TODO: Account for cards that need us to play two or more new colors to be played.
    //We cant play those kinds of cards this turn, but they still appear as such by
    //this algorithm.
    public ArrayList<Integer> getManaCurrent() {
        return getManaRemaining(new ArrayList<SpellAbility>());
    }


    /**
     * Puts our boolean values in our Integer ArrayList
     * @param b
     * @param i
     */
    /**private void appendBooleanValue(Boolean b, ArrayList<Integer> i) {
        if (b) {
            i.add(0);
        } else {
            i.add(1);
        }
    }
     */

    public void getMana() {
        getMana(ZoneType.Battlefield);
    }

    /**
     * Tallies our current on the battlefield mana
     * (or another zone if necessary, such as in deciding to mulligan),
     * and the possible colors of that mana.
     */
    public void getMana(ZoneType zone) {
        manaPool = 0;
        mountainNum = 0;
        swampNum = 0;
        islandNum = 0;
        forestNum = 0;
        plainsNum = 0;

        for (Card c: controller.getZone(zone)) {
            if (!c.isTapped()) {
                for (SpellAbility sa : c.getManaAbilities()) {
                    String type = sa.getMapParams().get("Produced");
                    if (type.contains(RED)) {
                        mountainNum += 1;
                        manaPool += 1;
                    }
                    if (type.contains(BLACK)) {
                        swampNum += 1;
                        manaPool += 1;
                    }
                    if (type.contains(BLUE)) {
                        islandNum += 1;
                        manaPool += 1;
                    }
                    if (type.contains(GREEN)) {
                        forestNum += 1;
                        manaPool += 1;
                    }
                    if (type.contains(WHITE)) {
                        plainsNum += 1;
                        manaPool += 1;
                    }

                }
                //Accounts for cards with multiple mana abiities, but can only use one per turn
                if (!c.getManaAbilities().isEmpty()) {
                    manaPool += 1 - c.getManaAbilities().size();
                }
            }
        }

    }

    /**
     * Checks to see if we can play an untapped land this turn, and if so, what colors we can gain
     * from doing so.
     * TODO: Reimplement this once we can ensure we play a land first.
     */

    public void checkPossibleColorPlays() {
        mountainAvaliable = false;
        swampAvaliable = false;
        forestAvaliable = false;
        plainsAvaliable = false;
        islandAvaliable = false;
        manaAvaliable = false;


        if (controller.getZone(ZoneType.Hand).contains(Card::isLand)
                && controller.getLandsPlayedThisTurn() == 0) {
            for (Card c : controller.getZone(ZoneType.Hand)) {
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
                manaPool += 1;
            }
        }
    }


}
