/**
 * Decision making class for how Ai is supposed to block
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since July 16, 2020
 */

package forge.game.research.decision.combat;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.research.card.Front;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.max;

public class Blocking {

    private final int REALLYHIGHVALUE = 50000;
    private ArrayList<Card> attackers;
    private ArrayList<Card> blockers;
    private int sack = 0;
    private int timeSave = 0;
    private final int LETHALAVOIDER = -50;
    private HashMap<ArrayList<Object>, Double> memo = new HashMap();

    public void Blocking() {
    }
    private Combat combat;
    private Front front;
    private Player defender;


    public Blocking(Player player, Combat inCombat){
        this.combat = inCombat;
        front = new Front();
        defender = player;
    }

    public void getBlocks(ArrayList<Card> aList, ArrayList<Card> bList) {
        attackers = aList;
        blockers = bList;
        Map<Card, ArrayList<Card>> combatMap = getChumpBlocks((knapsacking(attackers, blockers)));
        //AssignBlocks(combatMap);
        System.out.println("Blockers are valued:");
        for (Card card: bList) {
            System.out.println(card.getName() + " " + front.chooser(card));
        }
        System.out.println("");
        System.out.println("and Attackers are worth:");
        for (Card card: aList) {
            System.out.println(card.getName() + " " + front.chooser(card));
        }

        startSacking(bList.size(), bList, aList);
    }

    public void AssignBlocks(Map<Card, ArrayList<Card>> list){
        for (Card attacker: combat.getAttackers()) {
            for (Card blocker: list.get(attacker)){
                combat.addBlocker(attacker, blocker);
            }
        }
    }


    /**
     * Runs knapsack with lethality:
     *
     * @param attackers
     * @param blockers
     * @return
     */
    public Map<Card, ArrayList<Card>> knapsacking(ArrayList<Card> attackers, ArrayList<Card> blockers) {
        Map<Card, ArrayList<Card>> list = new HashMap<>();
        for (Card card: attackers) {
            list.put(card, new ArrayList<>());
        }
        return list;
    }


    public double knapsacking(int attackerToughness, Card attacker, ArrayList<Card> blockers) {
        ArrayList<Object> temp = new ArrayList<>();
        temp.add(attackerToughness);
        temp.add(attacker);
        temp.add(blockers);
        if (memo.containsKey(temp)) {
            return memo.get(temp);
        }


        if(blockers.size()==0 || attackerToughness<=0){
            return 0;
        }
        ArrayList<Card> combatlist = new ArrayList<Card>();
        combatlist.add(attacker);
        combatlist.add(blockers.get(blockers.size() - 1));
        if(evaluateBlock(combatlist) <= 0){
            return knapsacking(attackerToughness, attacker, blockers);
        }
        else{
            double toSave = max(
                    knapsacking(attackerToughness, attacker,
                            (ArrayList<Card>)blockers.subList(0, blockers.size()-1)),
                    knapsacking(attackerToughness - blockers.get(blockers.size()-1).getNetPower(),
                            attacker, (ArrayList<Card>)blockers.subList(0, blockers.size()-1))+evaluateBlock(combatlist)
            );
            memo.put(temp, toSave);
            return toSave;
        }

    }

    /**
     * Reassigns blockers if we have any excess blockers
     * @param list
     * @return
     */
    public Map<Card, ArrayList<Card>> getChumpBlocks (Map<Card, ArrayList<Card>> list) {

        ArrayList<Card> ableBlockers = getAbleBlockers(list);
        ableBlockers = getSortedList(ableBlockers);

        //Save the player first
        for (int i = 0; i < ableBlockers.size(); i++) {
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
        if( damage>= life) {
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
     * Calculates the value or 'goodness' of a block with the following:
     * - creature value of Ai  control dies
     * + creature value of opponent dies
     * + damage prevented
     * @param list : first card is attacker, rest of the cards are blockers
     * @return the 'goodnesss' of the block
     */
    public double evaluateBlock (ArrayList<Card> list) {
        //attacker related variables - who the attacker is (first card), the power and toughness of the attacker
        Card attacker = list.get(0);
        double attackerCurrentPower = attacker.getCurrentPower();
        double attackerCurrentHealth = attacker.getCurrentToughness();

        //inital value of a block is 0
        double blockVal = 0.0;
        ArrayList<Card> defenderList = getDefenderList(list);

        //checks to see if defender list is empty, if so, that attacker is not blocked, and the value of the block 0
        if (defenderList != null) {

            //seperate if statements are necessary, a creature whose equal to the remaining power/toughness
            //of the attacker can die (lose value) and kill the attacker (gain value). With multi blocks, happens
            //with the last creature usually
            for (Card defender : defenderList) {

                //if a defender's health is equal to or less than attacker (i.e. 2/2, 2/1, 3/2 vs a 2/2), that
                // creature 'dies'
                //value of a block decreases by the value of the defender we lose
                if (defender.getCurrentToughness() <= attackerCurrentPower) {
                    blockVal -= front.chooser(defender);
                }

                //if a defender's power is equal to or greater than attacker's health (i.e. 2/2, 3/2, 3/3 vs a 2/2),
                // that attacker 'dies'
                //value of a block increases by the value of the defender we lose
                //value of a block increases by the attack prevented
                if (defender.getCurrentPower() >= attackerCurrentHealth) {
                    blockVal += front.chooser(attacker);
                    blockVal += attacker.getCurrentPower();
                }

                //decreases the power, toughness that the attacker 'spends' to get through a block
                attackerCurrentPower -= defender.getCurrentToughness();
                attackerCurrentHealth -= defender.getCurrentPower();
            }
        }


        return blockVal;
    }

    /**
     * Creates a list of blockers to be used with evaluateBlock
     * @param list
     * @return
     */
    private ArrayList<Card> getDefenderList (ArrayList<Card> list) {
        Card opponentCrea = list.get(0);
        ArrayList<Card> defenderL = new ArrayList<>();

        //checks to make sure there are defenders to be had, if not the defender list will be empty, or 'null'
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

    //obsolete - to be deleted later if it is determined we do not need these methods
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
    private int multiSack(int blockNum, ArrayList<Card> bList, ArrayList<Attacker> aList) {
        sack += 1;

        ArrayList<Object> temp = new ArrayList<>();
        temp.add(blockNum);
        temp.add(bList);
        temp.add(aList);
        if (memo.containsKey(temp)) {
            timeSave += 1;
            return memo.get(temp).intValue();
        }

        ArrayList<Attacker> atks = new ArrayList<>();
        ArrayList<Integer> considering = new ArrayList<>();
        for (Attacker card: aList) {
            if (!card.isDead()) {
                atks.add(card);
            }
        }


        if (blockNum == 0 || atks.isEmpty()){
            return 0;
        }

        for (Attacker card: atks) {
            card.addDamage(bList.get(blockNum-1).getNetPower());
            considering.add(multiSack(blockNum - 1, bList, atks)
                    + getBlockValue(bList.get(blockNum-1), card, atks));
            card.subDamage(bList.get(blockNum-1).getNetPower());
        }

        considering.add(multiSack(blockNum - 1, bList, atks) + lethalCheck(atks));

        int toSave = getMax(considering);
        memo.put(temp, ((double) toSave));
        return toSave;
    }

    public void sackWrap(int blockNum, ArrayList<Card> bList, ArrayList<Attacker> aList) {
        for (int i = 0; i<bList.size(); i++) {

            ArrayList<Attacker> atks = new ArrayList<>();
            ArrayList<Integer> considering = new ArrayList<>();
            for (Attacker card : aList) {
                if (!card.isDead()) {
                    atks.add(card);
                }
            }


            if (blockNum == 0 || atks.isEmpty()) {
                return;
            }

            for (Attacker card : atks) {
                card.addDamage(bList.get(blockNum - 1).getNetPower());
                considering.add(multiSack(blockNum - 1, bList, atks)
                        + getBlockValue(bList.get(blockNum - 1), card, atks));
                card.subDamage(bList.get(blockNum - 1).getNetPower());
            }

            considering.add(multiSack(blockNum - 1, bList, atks) + lethalCheck(atks));
            int toSave = getMaxIndex(considering);
            if (!(toSave == atks.size())) {
                combat.addBlocker(atks.get(toSave).getSelf(), bList.get(blockNum - 1));
                atks.get(toSave).addDamage(bList.get(blockNum - 1).getNetPower());
            }
            blockNum -= 1;
        }
    }

    public int lethalCheck(ArrayList<Attacker> atks) {
        int damage = 0;
        for (Attacker selected: atks) {
            //TODO improve blocking check
            if (selected.getDamage() == 0) {
                damage += selected.getSelf().getNetPower();
            }
        }

        if (damage>=defender.getLife()) {
            return LETHALAVOIDER;
        } else {
            return 0;
        }
    }

    public void startSacking(int size, ArrayList<Card> blocks, ArrayList<Card> attacks) {
        ArrayList<Attacker> atks = new ArrayList<>();
        for (Card card: attacks) {
            atks.add(new Attacker(card));
        }
        sackWrap(size, blocks, atks);
    }

    public int getMax(ArrayList<Integer> ints) {
        int max = ints.get(0);
        int index = 0;
        for (int i = 0; i<ints.size(); i++) {
            if (ints.get(i)>max) {
                max = ints.get(i);
                index = i;
            }
        }
        return max;
    }

    public int getMaxIndex(ArrayList<Integer> ints) {
        int max = ints.get(0);
        int index = 0;
        for (int i = 0; i<ints.size(); i++) {
            if (ints.get(i)>max) {
                max = ints.get(i);
                index = i;
            }
        }
        return index;
    }

    public int getBlockValue(Card block, Attacker atk, ArrayList<Attacker> atks) {
        //We should use doubles if this is inaccurate, but this does save space
        int blockVal = 0;
        if (atk.getHealthRemaining() + block.getNetPower() < 0) {
        } else if (atk.isDead()) {
            blockVal += front.chooser(atk.getSelf());
        }
        //TODO: Checks if the attacker could kill this creature, but doesn't account for
        //other targets of the damage
        if (atk.getSelf().getNetPower() >= block.getNetToughness()) {
            blockVal -= front.chooser(block);
        }

        //TODO: Checks if we are the only blocker, but doesn't account for walls
        if (atk.damage == block.getNetPower()) {
            blockVal += targetHealthVal(combat.getDefenderByAttacker(atk.getSelf()),
                    atk.getSelf().getNetPower());
        }

        blockVal+= lethalCheck(atks);

        return blockVal;
    }

    public class Attacker {
        int health;
        int damage;
        Card self;
        public Attacker(Card card) {
            self = card;
            damage = 0;
            health = card.getNetToughness();
        }

        public boolean isDead() {
            return (health <= damage);
        }

        public int getDamage() {
            return damage;
        }

        public int getHealth() {
            return health;
        }

        public int getHealthRemaining() {
            return health - damage;
        }

        public Card getSelf() {
            return self;
        }

        public void addDamage(int add) {
            damage += add;
        }

        public void subDamage(int add) {
            damage -= add;
        }
    }

}
