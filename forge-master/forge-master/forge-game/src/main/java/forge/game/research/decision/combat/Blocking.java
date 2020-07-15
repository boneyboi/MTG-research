package forge.game.research.decision.combat;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.research.card.Front;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Double.POSITIVE_INFINITY;

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


    public void Blocking(Player player, Combat inCombat){
        this.combat = inCombat;
        front = new Front();
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
        ArrayList<Card> toReplace = new ArrayList<Card>();

        for (Card key : list.keySet()) {

            if (key.getCurrentToughness() > totalPowerOfBlock(list.get(key))) {
                if (list.get(key).size() > 1) {
                    toReplace.add(lowestValueCard(list.get(key)));
                    editedMap.put(key, toReplace);
                }
            }

        }

        return editedMap;
    }

    /**
     *
     * @param list
     * @return
     */
    private Card lowestValueCard (ArrayList<Card> list) {
        Card lowValCard = null;
        double minVal = POSITIVE_INFINITY;

        for (Card card : list) {
            if (front.chooser(card) < minVal) {
                lowValCard = card;
                minVal = front.chooser(card);
            }
        }

        return lowValCard;
    }

    /**
     *
     * @param list
     * @return
     */
    private int totalPowerOfBlock(ArrayList<Card> list) {
        int totalPower = 0;

        for (Card card : list) {
            totalPower += card.getCurrentPower();
        }

        return totalPower;
    }

    /**
     * Returns whether or not there is excess blockers
     * @return true if there are any excess blockers
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
                        GameEntity target = combat.getDefenderByAttacker(card);
                        if (target instanceof Card) {
                            //TODO: Does current power adjust for counters?
                            priority = targetHealthVal(((Card) target).getCurrentLoyalty(), card.getCurrentPower());
                        } else if (target instanceof Player) {
                            priority = targetHealthVal(((Player) target).getLife(), card.getCurrentPower());
                        }
                        if (priority> max) {
                            max = priority;
                            attacker = card;
                        }
                    }
                }
            }
            if (max>=front.chooser(c) || isLethal(list, combat.getDefenderByAttacker(attacker))) {
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

    public int targetHealthVal(int life, int damage) {
        if( damage> life) {
            return REALLYHIGHVALUE;
        }
        return damage;
    }
}
