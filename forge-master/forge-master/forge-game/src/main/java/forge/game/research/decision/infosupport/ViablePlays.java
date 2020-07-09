/**
 * Determines what cards can be played with the mana available
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.decision.infosupport.removal.RemovalChecker;
import forge.game.research.decision.infosupport.removal.RemovalList;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class ViablePlays {

    ArrayList<SpellAbility> plays = new ArrayList<>();
    ArrayList<Card> lands;
    Player controller = null;
    ArrayList<Integer> manapool;
    public int whiteMana;
    public int blueMana;
    public int blackMana;
    public int redMana;
    public int greenMana;


    public ViablePlays(Player p) {
        controller = p;
    }

    /**
     * Returns the lands that can be played from the hand.
     * @return ArrayList<Card>
     */
    public ArrayList<Card> getLandPlays() {
        lands = new ArrayList<Card>();
        addLandOptions();
        return lands;
    }

    /**
     * Returns the nonland options we have.
     * @return ArrayList<SpellAbility>
     */
    public ArrayList<SpellAbility> getNonlandPlays() {
        emptyOptions();
        setManaPool(false);
        buildOptions();
        return plays;
    }

    public ArrayList<SpellAbility> getNonlandPlaysAfter(ArrayList<SpellAbility> sa){
        emptyOptions();
        setManaAfter(sa);
        buildOptions();
        for (SpellAbility spell: sa) {
            plays.remove(spell);
        }
        return plays;
    }

    public ArrayList<SpellAbility> getPotentialPlays() {
        emptyOptions();
        setManaPool(true);
        buildOptions();
        return plays;
    }

    /**
     * Determines the number of plays possible with a current hand
     * This is for mulliganing purposes
     * @return
     */
    public ArrayList<SpellAbility> getPossibilities(ArrayList<Card> cards) {
        emptyOptions();
        buildPossibleOptions(cards);
        return plays;
    }


    /**
     * Determines whether a player has the necessary colors to pay the cost for a card.
     * @param sa
     * @return boolean
     */
    private boolean areColorsAvaliable(SpellAbility sa) {
        int[] shardsNeeded = sa.getPayCosts().getTotalMana().getColorShardCounts();
        if (shardsNeeded[0] > whiteMana ||
                shardsNeeded[1] > blueMana ||
                shardsNeeded[2] > blackMana ||
                shardsNeeded[3] > redMana ||
                shardsNeeded[4] > greenMana) {
            return false;
        }
        return true;
    }

    /**
     * Adds all playable abilities from a zone to the pool of options
     * @param z
     */
    private void addZoneOptions(ZoneType z) {
        //TODO: Fix exile issue
        for (Card c: controller.getZone(z)) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= (int) manapool.get(0) &&
                        sa.canPlay() &&
                        areColorsAvaliable(sa) &&
                        canTarget(sa)) {
                    plays.add(sa);
                }
            }
        }
    }

    private void addCardOptions(ArrayList<Card> cards) {
        for (Card c: cards) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= (int) manapool.get(0) &&
                        areColorsAvaliable(sa)) {
                    plays.add(sa);
                }
            }
        }
    }

    /**
     * Adds the possible land plays to our list of options
     */
    private void addLandOptions(){
        for (Card c: controller.getZone(ZoneType.Hand)) {
            if (c.isLand()) {
                lands.add(c);
            }
        }
    }

    private boolean canTarget(SpellAbility sa) {
        RemovalChecker rc = new RemovalChecker();
        ArrayList<Card> options = new ArrayList<>();
        RemovalList list = new RemovalList();

        if (!sa.isTargetNumberValid()) {
            for (Player p: controller.getGame().getPlayers()) {
                if (sa.canTarget(p)) {
                    return true;
                }
            }
            if (!rc.shouldTargetOthers(sa.getHostCard())){
                for (Card c: controller.getZone(ZoneType.Battlefield)) {
                    options.add(c);
                }
            } else {
                for (Card c: controller.getOpponents().get(0).getZone(ZoneType.Battlefield)) {
                    options.add(c);
                }
            }
            Card chosen = list.getBiggestThreat(options);
            if(sa.canTarget(chosen)) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Assembles the list of all possible plays the player can make
     */
    private void buildOptions() {
        whiteMana = manapool.get(1);
        blueMana = manapool.get(2);
        blackMana = manapool.get(3);
        redMana = manapool.get(4);
        greenMana = manapool.get(5);

        addZoneOptions(ZoneType.Hand);
        addZoneOptions(ZoneType.Battlefield);
        addZoneOptions(ZoneType.Graveyard);
        addZoneOptions(ZoneType.Exile);
    }

    private void buildPossibleOptions(ArrayList<Card> cards) {
        ManaEvaluation manaOptions = new ManaEvaluation(controller);
        manapool = manaOptions.getManaFromCards(cards);
        whiteMana = manapool.get(1);
        blueMana = manapool.get(2);
        blackMana = manapool.get(3);
        redMana = manapool.get(4);
        greenMana = manapool.get(5);
        addCardOptions(cards);
    }

    public void setManaAfter(ArrayList<SpellAbility> sa) {
        ManaEvaluation manaOptions = new ManaEvaluation(controller);
        manapool = manaOptions.getManaRemaining(sa);
    }

    public void setManaPool(boolean withLand) {
        ManaEvaluation manaOptions = new ManaEvaluation(controller);
        if (withLand) {
            manapool = manaOptions.getManaPossible();
        } else {
            manapool = manaOptions.getManaCurrent();
        }
    }

    private void emptyOptions() {
        plays = new ArrayList<SpellAbility>();
    }

}
