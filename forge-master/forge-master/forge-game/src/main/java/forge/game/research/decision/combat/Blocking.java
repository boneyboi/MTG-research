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
    private Player defender;


    public Blocking(Player player, Combat inCombat){
        this.combat = inCombat;
        front = new Front();
        defender = player;
    }


    public Map<Card, ArrayList<Card>> getBlocks(ArrayList<Card> aList, ArrayList<Card> bList) {
        attackers = aList;
        blockers = bList;
        return getChumpBlocks((knapsacking(attackers, blockers)));
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
    /**
    public Map<Card, ArrayList<Card>> removeExcessBlockers(Map<Card, ArrayList<Card>> list) {
        Map<Card, ArrayList<Card>> editedMap = list;
        ArrayList<Card> toReplace = new ArrayList<Card>();

        for (Card key : list.keySet()) {

            if (key.getCurrentToughness() > totalPowerOfBlock(list.get(key))
                && list.get(key).size() > 1) {
                    toReplace.add(lowestValueCard(list.get(key)));
                    editedMap.put(key, toReplace);
                }
            }


        return editedMap;
    }
     */


    /**
     *
     * @param list
     * @return
     */
    /**
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
     */


    /**
     * Reassigns blockers if we have any excess blockers
     * @param list
     * @return
     */
    public Map<Card, ArrayList<Card>> getChumpBlocks (Map<Card, ArrayList<Card>> list) {

        ArrayList<Card> ableBlockers = getAbleBlockers(list);
        ableBlockers = getSortedList(ableBlockers);

        //Save the player first
        for (int i = 0; i == ableBlockers.size(); i++) {
            //Blockers are ordered weakest first, so we will block weakest to strongest
            Card blocker = ableBlockers.get(i);
            int max = 0;
            Card attacker = null;

            //Find the strongest damage we can block
            for (Card card: attackers) {
                if (list.get(card).isEmpty()) {
                    int priority = 0;
                    if (defender == combat.getDefenderByAttacker(card)) {
                        //TODO: Account for trample here.
                        priority = targetHealthVal(defender, card.getCurrentPower());
                    }
                    if (priority> max) {
                        max = priority;
                        attacker = card;
                    }
                }
            }
            //The second condition is not redundant because it evaluates all damage we are taking
            //not just the damage from that one attacker.
            if (max>=front.chooser(blocker) || isLethal(list, defender)) {
                ArrayList<Card> temp = new ArrayList<>();
                temp.add(blocker);
                list.replace(attacker, temp);
            }
        }
        //TODO: chump block for planeswalkers


        return list;
    }

    public ArrayList<Card> getAbleBlockers(Map<Card, ArrayList<Card>> list) {
        ArrayList<Card> returning = new ArrayList<>();
        for (Card card: blockers) {
            if (!list.containsValue(card)) {
                returning.add(card);
            }
        }
        return returning;
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
            //We should never reach this case
            System.out.print("The defending unit is not a planeswalker or player");
        }
        if( damage> life) {
            return REALLYHIGHVALUE;
        }
        return damage;
    }

    /**
     * Sorts the incoming battlefield list into a list that is organized from minimum value to max value
     * @param list - list of cards on the opponent's battlefield
     * @return removalList
     */
    public ArrayList<Card> getSortedList(ArrayList<Card> list) {
        ArrayList<Card> returnList = new ArrayList<>();
        int count = 0;

        //battlefield list is not empty/there is something on your opponent's side of the field
        if (list.size() > 0) {

            //sorting algorithm
            do {
                //assures that we initially always have a min
                double minVal = POSITIVE_INFINITY;
                Card cardToAdd = null;

                for (Card card : list) {
                    //iterates and find a card with the least value, which is added first
                    //makes sure duplicates cannot be added
                    if (front.chooser(card) <= minVal && !returnList.contains(card)) {
                        cardToAdd = card;
                        minVal = front.chooser(card);
                    }
                }
                //may remove this clause since 0 is protected against
                if (cardToAdd != null) {
                    returnList.add(cardToAdd);
                }
                count++;

                //as many times as there are cards
            } while (count < list.size());
        }

        return returnList;
    }

    /**
     * @param list - first card is attacker, rest of the cards are blockers
     * - creature value of Ai  control dies
     * + creature value of opponent dies
     * + damage prevented
     */
    public double evaluateBlock (ArrayList<Card> list) {
        //attacker related variables
        Card attacker = list.get(0);
        double attackerCurrentPower = attacker.getCurrentPower();
        double attackerCurrentHealth = attacker.getCurrentToughness();

        double blockVal = 0.0;
        ArrayList<Card> defenderList = getDefenderList(list);

        if (defenderList != null) {
            for (Card defender : defenderList) {
                attackerCurrentPower -= defender.getCurrentToughness();
                attackerCurrentHealth -= defender.getCurrentPower();

                if (defender.getCurrentToughness() < attackerCurrentPower) {
                    blockVal -= front.chooser(defender);
                } else if (defender.getCurrentPower() > attackerCurrentHealth) {
                    blockVal += front.chooser(attacker);
                    blockVal += attacker.getCurrentPower();
                }
            }
        }


        return blockVal;
    }

    private ArrayList<Card> getDefenderList (ArrayList<Card> list) {
        Card opponentCrea = list.get(0);
        ArrayList<Card> defenderL = new ArrayList<>();

        if (list.size() > 1) {
            for (Card defender : list) {
                if (defender != opponentCrea) {
                    defenderL.add(defender);
                }
            }
        }
        else {
            defenderL = null;
        }

        return defenderL;
    }
}
