/**
 * Subclass of CardEvaluator - determines the value of cards with the Creature 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.card;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;

public class CreatureEval extends CardEvaluator {

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
    public static final double FLASHVAL = 44.5;
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

    public CreatureEval() {
        super();
    }



    /**
     * A card's value depends on its:
     *  rarity
     *  number of colors
     *  number of colored mana symbols in its cost
     *  CMC
     *  base value (.5 to account for 0-cost creatures)
     * @param card - card object
     * @return the value of the card
     */

    @Override
    public final double evaluate (Card card) {
        double stats = getStatTotal(card);
        double keyword = getKeywordValue(card);
        double counters = getCounters(card);
        double Cardvalue = (BASE + stats + keyword + counters);
        double value = (Cardvalue) * getRareMultiplier(card);

        return value;
    }

    public double getStatTotal(Card card) {
        return card.getCurrentPower()
                + card.getCurrentToughness();
    }

    public double getStatChange (Card card){
        return this.getPowerChange(card) +
               this.getToughnessChange(card);
    }

    public double getPowerChange (Card card){
        if (card.getCopiedPermanent() != null) {
            return card.getCurrentPower() - card.getPaperCard().getRules().getMainPart().getIntPower();
        } else {
            return card.getCurrentPower() - card.getBasePower();
        }
    }


    public double getToughnessChange (Card card){
        if (card.getCopiedPermanent() != null) {
            return card.getCurrentToughness() - card.getPaperCard().getRules().getMainPart().getIntToughness();
        } else {
            return card.getCurrentToughness() - card.getBaseToughness();
        }
    }

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
     *
     * @param card
     * @return keyValue
     */
    public double getKeywordValue (Card card) {
        double keyValue = 0;
        Keyword key = null;

        for (KeywordInterface k : card.getKeywords()){
            key = k.getKeyword();

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

        return keyValue;
    }


}
