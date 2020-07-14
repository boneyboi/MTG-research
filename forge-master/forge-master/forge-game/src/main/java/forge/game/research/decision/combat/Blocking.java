package forge.game.research.decision.combat;

import forge.game.card.Card;
import forge.game.combat.Combat;

import java.util.ArrayList;
import java.util.Map;

public class Blocking {

    private Map<ArrayList, Integer> cache;
    private Combat combat;

    public void Blocking(Combat inCombat){
        this.combat = inCombat;
    }

    public Map<Card, ArrayList<Card>> getBlocks(ArrayList<Card> attackers, ArrayList<Card> blockers) {
        return getChumpBlocks(removeExcessBlockers(knapsacking(attackers, blockers)));
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

    }

    /**
     * Reassigns blockers if we have any excess blockers
     * @param list
     * @return
     */
    public Map<Card, ArrayList<Card>> getChumpBlocks (Map<Card, ArrayList<Card>> list) {

    }
}
