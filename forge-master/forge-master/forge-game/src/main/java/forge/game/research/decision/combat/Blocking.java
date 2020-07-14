package forge.game.research.decision.combat;

import forge.game.card.Card;
import forge.game.combat.Combat;

import java.util.ArrayList;
import java.util.Map;

public class Blocking {

    private final int REALLYHIGHVALUE = 50000;

    public void Blocking(){
    private Map<ArrayList, Integer> cache;
    private Combat combat;
    private int excessBlockers;

    public void Blocking(Combat inCombat){
        this.combat = inCombat;
    }

    public Map<Card, ArrayList<Card>> getBlocks(ArrayList<Card> attackers, ArrayList<Card> blockers) {
        return getChumpBlocks(removeExcessBlockers(knapsacking(attackers, blockers)), blockers);
    }

    /**
     * Runs knapsack with lethality:
     *
     * @param attackers
     * @param blockers
     * @return
     */
    public Map<Card, ArrayList<Card>> knapsacking(ArrayList<Card> attackers, ArrayList<Card> blockers) {

    }

    /**
     * Checks to see if attacking creature doesn't die, remove all blockers except for
     * weakest creature
     * @param list
     * @return
     */
    public Map<Card, ArrayList<Card>> removeExcessBlockers(Map<Card, ArrayList<Card>> list) {
        Map<Card, ArrayList<Card>> editedMap = list;

        return editedMap;
    }

    /**
     * Returns whether or not there is excess blockers
     * @return
     */
    private boolean areExcessBlockers() {

        if (excessBlockers > 0) {
            return true;
        }
        return false;
    }

    /**
     * Reassigns blockers if we have any excess blockers
     * @param list
     * @return
     */
    public Map<Card, ArrayList<Card>> getChumpBlocks (Map<Card, ArrayList<Card>> list) {

        return list;
    }

    public int targetHealthVal(int life, int damage) {
        if( damage> life) {
            return REALLYHIGHVALUE;
        }
        return damage;
    }
}
