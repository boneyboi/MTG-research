/**
 * Subclass of CardEvaluator - determines the value of cards with the Creature 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 26, 2020
 */

package forge.game.research.card;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;

public class CreatureEval extends CardEvaluator {

    //non-constants
    private double KeywordsMul = 1.0;

    //constants for keywords
    public static final double INDESTRUCTIBLEVAL = 10;
    public static final double PROTECITONVAL = 9.5;
    public static final double HEXPROOFVAL = 9;
    public static final double DOUBLESTRIKEVAL = 7.5;
    public static final double FLYINGVAL = 6.5;
    public static final double HASTEVAL = 5.5;
    public static final double FIRSTSTRIKEVAL = 5.3;
    public static final double DEATHTOUCHVAL = 5;
    public static final double PROWESSVAL = 4.5;
    public static final double FLASHVAL = 4.5;
    public static final double VIGILIANCEVAL = 3.8;
    public static final double TRAMPLEVAL = 3.5;
    public static final double LIFELINKVAL = 3;
    public static final double MENACEVAL = 2;
    public static final double REACHVAL = 1.5;
    public static final double DEFENDERVAL = -2;

    public static final double BASE = .5;
    public static final int STATBOOST2 = 2;
    public static final int STATBOOST3 = 3;
    public static final int STATBOOST4 = 4;

    //calls card evaluator constructor
    public CreatureEval() {
        super();
    }



    /**
     * A creatures's value depends on its:
     *  keywords with number of keywords
     *  base value (.5 to account for 0-cost creatures)
     *  power and toughness (current)
     *  counters
     * @param card - card object
     * @return the value of the card
     */

    @Override
    public final double evaluate (Card card) {
        double totalStats = getStatTotal(card);
        double keyword = getKeywordValue(card);
        double counters = getCounters(card);
        double abilities = calculateAbilityVal(card);
        double Cardvalue = (BASE + totalStats + keyword + counters + abilities);
        double value = (Cardvalue) * KeywordsMul;

        return value;
    }

    /**
     * Calculates the total stats (power and toughness) for a card
     * @param card
     * @return
     */
    public double getStatTotal (Card card) {
        double statTotal = card.getCurrentPower() + card.getCurrentToughness();

        return statTotal;
    }

    /**
     * Gets the types of counters on a card and then multiply those counters by a value associated with
     * those specific counters (i.e. -P/-T is worth -2)
     * @param card
     * @return
     */
    public double getCounters (Card card) {
        return  card.getCounters(CounterType.P1P1)*STATBOOST2 +
                card.getCounters(CounterType.P1P0) +
                card.getCounters(CounterType.P0P1) +
                card.getCounters(CounterType.P2P2)*STATBOOST4 +
                card.getCounters(CounterType.P1P2)*STATBOOST3 +
                card.getCounters(CounterType.P0P2)*STATBOOST2 +
                card.getCounters(CounterType.P2P0)*STATBOOST2 -
                card.getCounters(CounterType.M1M1)*STATBOOST2 +
                card.getCounters(CounterType.M1M0) -
                card.getCounters(CounterType.M0M1) -
                card.getCounters(CounterType.M0M2)*STATBOOST2 -
                card.getCounters(CounterType.M2M2)*STATBOOST4 -
                card.getCounters(CounterType.M2M1)*STATBOOST3;
    }

    /**
     * Calculates the portion of a card's value that is reliant on keywords,
     * cumulative with multiple keywords,
     * multiplied by a multiplier when a card has more than one
     * @param card
     * @return keyValue
     */
    public double getKeywordValue (Card card) {
        double keyValue = 0;
        Keyword key = null;

        for (KeywordInterface k : card.getKeywords()){
            key = k.getKeyword();
            this.KeywordsMul += .05;

            //looks for the keyword listed on the card, adds associated value
            if (key.equals(Keyword.INDESTRUCTIBLE)) {
                keyValue += INDESTRUCTIBLEVAL;
            } else if (key.equals(Keyword.PROTECTION)) {
                keyValue += PROTECITONVAL;
            } else if (key.equals(Keyword.HEXPROOF)) {
                keyValue += HEXPROOFVAL;
            } else if (key.equals(Keyword.DOUBLE_STRIKE)) {
                keyValue += DOUBLESTRIKEVAL;
            } else if (key.equals(Keyword.FLYING)) {
                keyValue += FLYINGVAL;
            } else if (key.equals(Keyword.HASTE)) {
                keyValue += HASTEVAL;
            } else if (key.equals(Keyword.FIRST_STRIKE)) {
                keyValue += FIRSTSTRIKEVAL;
            } else if (key.equals(Keyword.DEATHTOUCH)) {
                keyValue += DEATHTOUCHVAL;
            } else if (key.equals(Keyword.PROWESS)) {
                keyValue += PROWESSVAL;
            } else if (key.equals(Keyword.FLASH)) {
                keyValue += FLASHVAL;
            } else if (key.equals(Keyword.VIGILANCE)) {
                keyValue += VIGILIANCEVAL;
            } else if (key.equals(Keyword.TRAMPLE)) {
                keyValue += TRAMPLEVAL;
            } else if (key.equals(Keyword.LIFELINK)) {
                keyValue += LIFELINKVAL;
            } else if (key.equals(Keyword.MENACE)) {
                keyValue += MENACEVAL;
            } else if (key.equals(Keyword.REACH)) {
                keyValue += REACHVAL;
            } else if (key.equals(Keyword.DEFENDER)) {
                keyValue += DEFENDERVAL;
            }
        }

        //double testMul = numKeywordsMul;
        return keyValue;
    }

    public double calculateAbilityVal (Card card) {
        double abilityVal = 0.0;

        abilityVal += hasPassiveAbility(card) + hasTriggerAbility(card) + hasActivateAbility(card);

        return abilityVal;
    }

    private double hasTriggerAbility (Card card) {
        double trigVal = 0;

        //5.3
        trigVal += card.getTriggers().size() * FIRSTSTRIKEVAL;

        return trigVal;
    }

    private double hasPassiveAbility (Card card) {
        int numPass = 0;
        double passVal= 0.0;


        for (StaticAbility sa : card.getStaticAbilities()) {
            if (sa.hasParam("Mode") && sa.getParam("Mode").equals("Continuous")) {
                numPass += 1;
            }
        }

        //7.5
        passVal += (numPass * DOUBLESTRIKEVAL);

        return passVal;
    }

    private double hasActivateAbility (Card card) {
        int numActi = 0;
        double actiVal = 0.0;

        for (SpellAbility sa : card.getSpellAbilities()) {
            if (sa.hasParam("Cost")) {
                numActi += 1;
            }
        }

        //3.5
        actiVal += (numActi * TRAMPLEVAL);

        return actiVal;
    }


}