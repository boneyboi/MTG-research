package forge.game.research.decision.combat;

import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.research.card.Front;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Attacking {
    private static Front front= new Front();
    private static Blocking blockobj;
    private static Player defender;
    private static Player attacker;
    private static Combat inCombat;
    private static ArrayList<Card> blockers = new ArrayList<Card>();
    private static ArrayList<Card> attackers = new ArrayList<Card>();
    public Attacking(Player attacker, Player defender, Combat inCombat){
        this.attacker = attacker;
        this.defender = defender;
        blockobj = new Blocking(defender, inCombat);
        this.inCombat = inCombat;
    }
    //return a list of attackers and their respective defenders(face or planesawalker)

    public void declareAttack() {
        setAttackers(chooseAttackers(this.attacker, this.defender));
    }

    //prune attacking cards with can attack method
    //
    public HashMap<Card, GameEntity> chooseAttackers(Player attacker, Player defender){
        HashMap<Card, GameEntity> resultattackers = new HashMap<Card, GameEntity>();
        //for card c in creatures in play
        //if(combatutil.canblock(card, combat)
        //if it can add it to blockers

        for(Card card : defender.getCreaturesInPlay()){
            if(CombatUtil.canBlock(card, inCombat)){
                blockers.add(card);
            }
        }
        for(Card card : attacker.getCreaturesInPlay()){
            if(CombatUtil.canAttack(card)){
                attackers.add(card);
            }
        }
        for(Card card : knapsack2(defender.getLife(), attackers, new ArrayList<List<Card>>(), new ArrayList<Card>(), blockers)){
            if(!card.equals("")){
                resultattackers.put(card, defender);
            }
        }

        return resultattackers;
    }

    public void setAttackers(HashMap<Card, GameEntity> map) {
        for (Card c: map.keySet()) {
            if(!c.getName().equals("")) {
                try{
                    inCombat.addAttacker(c, map.get(c));
                } catch(NullPointerException e){
                    System.out.println("Null card exception");
                }

            }
        }
    }

    public static ArrayList<Card> knapsack2(int life, List<Card> arrlist2, List<List<Card>> resultlist, List<Card> resultitems, List<Card> blockers){
        if(arrlist2.size()==0 || life == 0){
            /*if(weight==0) {
                resultlist.add(List.copyOf(resultitems));
                resultitems.clear();
            }*/
            if(life==0){
                //knapsackcount++;
                //System.out.println(resultlist);
            }
            ArrayList<Card> basecase = new ArrayList<Card>();
            basecase.add(new Card(0, null));
            return basecase;
        }
        /*if(arrlist2.get(arrlist2.size()-1)==weight){
            //assume we will add the item b/c we did not skip it
            resultitems.add(arrlist2.get(arrlist2.size()-1));
        }*/
        //we can actually go over in life
        //a condition to consider might be to not attack bc the life they lose is less than the value we lose
        int battlesum = 0;
        HashMap<Card, ArrayList<Card>> simblockers = new HashMap<Card, ArrayList<Card>>();
        if(resultlist.size()>0){
            simblockers = (HashMap<Card, ArrayList<Card>>)blockobj.startSacking(blockers.size(),addLastAttacker(arrlist2.get(arrlist2.size()-1),
                    (ArrayList<Card>)resultlist.get(resultlist.size()-1)),(ArrayList<Card>)blockers);
        } else {
            ArrayList<Card> templistcard = new ArrayList<Card>();
            templistcard.add(arrlist2.get(arrlist2.size()-1));
            simblockers = (HashMap<Card, ArrayList<Card>>)blockobj.startSacking(
                            blockers.size(),templistcard,(ArrayList<Card>)blockers);
        }


        for(Card card : simblockers.keySet()){
            battlesum += evaluateBlock(card, simblockers.get(card));
        }
        if(battlesum < 0) { //condition to prevent addition of an object
            return knapsack2(life, arrlist2.subList(0, arrlist2.size()-1), resultlist, resultitems, blockers);
        }else{
            ArrayList<Card> skip = knapsack2(life, arrlist2.subList(0, arrlist2.size()-1),resultlist, resultitems, blockers);
            ArrayList<Card> take = (ArrayList<Card>)knapsack2(life-arrlist2.get(arrlist2.size()-1).getNetPower(), arrlist2.subList(0, arrlist2.size()-1),
                    resultlist,resultitems, blockers);
            //System.out.println(arrlist2.get(arrlist2.size()-1));
            //System.out.println(skip);
            //replace this condition with:
            //if the evaluation of blocking the skip is less than blocking the added attacker
            if(evaluateBlock(null, skip) < evaluateBlock(arrlist2.get(arrlist2.size()-1), take)){
                take.add(arrlist2.get(arrlist2.size()-1));
                resultlist.add(take);
                return take;
            } else {
                resultlist.add(skip);
                return skip;
            }
            /*int tempresult = max(knapsack(weight, arrlist2.subList(0, arrlist2.size()-1),resultlist, resultitems),
                    putintinlist(arrlist2.get(arrlist2.size()-1),resultitems) + knapsack(weight-arrlist2.get(arrlist2.size()-1), arrlist2.subList(0, arrlist2.size()-1),resultlist,resultitems));
            System.out.println(arrlist2.get(arrlist2.size()-1));
            if(weight!=0){
                resultitems.clear();
            }
            return tempresult;*/
            //goal should be add successful knapsacks(the combination = weight) to the list
        }
    }
    public static ArrayList<Card> addLastAttacker(Card currentAttacker, ArrayList<Card> attackers){
        ArrayList<Card> tempattackers = new ArrayList<Card>(attackers);
        tempattackers.add(currentAttacker);
        return tempattackers;
    }
    public static int sum(List<Integer> inputlist){
        int sum= 0;
        for(Integer i : inputlist){
            sum+=i;
        }
        return sum;
    }

    public static Integer putintinlist(Integer num, List<Integer> intlist){
        intlist.add(num);
        return num;
    }

    public static double evaluateBlock(Card atk, ArrayList<Card> block) {
        if(atk==null){
            return 0;
        }
        ArrayList<Card> temp = new ArrayList<>();
        temp.add(atk);
        temp.addAll(block);
        return evaluateBlock(temp);
    }
    public static double evaluateBlock(ArrayList<Card> list) {
        //attacker related variables - who the attacker is (first card), the power and toughness of the attacker
        Card attacker = list.get(0);
        double attackerCurrentPower = attacker.getNetPower();
        double attackerCurrentHealth = attacker.getNetToughness();

        //initial value of a block is 0
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
                if (defender.getNetToughness() <= attackerCurrentPower) {
                    blockVal -= front.chooser(defender);
                }

                //if a defender's power is equal to or greater than attacker's health (i.e. 2/2, 3/2, 3/3 vs a 2/2),
                // that attacker 'dies'
                //value of a block increases by the value of the defender we lose
                //value of a block increases by the attack prevented
                if (defender.getNetPower() >= attackerCurrentHealth && attackerCurrentHealth > 0) {
                    blockVal += front.chooser(attacker);
                }

                //decreases the power, toughness that the attacker 'spends' to get through a block
                attackerCurrentPower -= defender.getNetToughness();
                attackerCurrentHealth -= defender.getNetPower();
            }

            blockVal += attacker.getNetPower();
        }
        //for(Card card : knapsack2(defender.getLife(), /*get attackers*/, new ArrayList<List<Card>>(), new ArrayList<Card>(), /*get blockers*/){
        //    attackers.put(card, );
        //}

        return blockVal;
    }
    private static ArrayList<Card> getDefenderList(ArrayList<Card> list) {
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
}
