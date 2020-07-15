package forge.game.research.decision.combat;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.research.card.Front;

import java.util.ArrayList;
import java.util.Map;

public class Blocking {

    private final int REALLYHIGHVALUE = 50000;
    private ArrayList<Card> attackers;
    private ArrayList<Card> blockers;

    public void Blocking() {
    }
    private Map<ArrayList, Integer> cache;
    private Combat combat;
    private int excessBlockers;
    private Front front;
    private Player defender;



    public void Blocking(Combat inCombat, Player defendingPlayer){
        this.combat = inCombat;
        front = new Front();
        defender = defendingPlayer;
    }

    public Map<Card, ArrayList<Card>> getBlocks(ArrayList<Card> aList, ArrayList<Card> bList) {
        attackers = aList;
        blockers = bList;
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
        return null;
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
        for (Card c: blockers) {
            int max = 0;
            Card attacker = null;
            if (!list.containsValue(c)) {
                for (Card card: attackers) {
                    if (list.get(card).isEmpty()) {
                        int priority = 0;
                        if (defender == combat.getDefenderByAttacker(card)) {
                            priority = targetHealthVal(defender, card.getCurrentPower());
                        }
                        if (priority> max) {
                            max = priority;
                            attacker = card;
                        }
                    }
                }
            }
            if (max>=front.chooser(c) || isLethal(list, defender)) {
                ArrayList<Card> temp = new ArrayList<>();
                temp.add(c);
                list.replace(attacker, temp);
            }
        }
        return list;
    }

    public boolean isLethal(Map<Card, ArrayList<Card>> list, GameEntity target) {
        int life;
        if (target instanceof Card) {
            life = ((Card) target).getCurrentLoyalty();
        } else if (target instanceof Player) {
            life = ((Player) target).getLife();
        } else {
            //We should never reach this case.
            life = 100;
        }
        return takingDamage(list, target) >= life;
    }

    public int takingDamage(Map<Card, ArrayList<Card>> list, GameEntity target) {
        int damage = 0;
        for (Card c: list.keySet()) {
            if (list.get(c).isEmpty() && target == combat.getDefenderByAttacker(c)) {
                damage += c.getCurrentPower();
            }
        }
        return damage;
    }

    public int targetHealthVal(GameEntity target, int damage) {
        int life = 0;
        if (target instanceof Card) {
            life = ((Card) target).getCurrentLoyalty();
        } else if (target instanceof Player) {
            life = ((Player) target).getLife();
        } else {
            System.out.print("The defending unit is not a planeswalker or player");
        }
        if( damage> life) {
            return REALLYHIGHVALUE;
        }
        return damage;
    }
}
