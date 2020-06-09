/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

package forge.game.research;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class CreatureEval extends CardEvaluator {

    //constants - first four are rarity
    public static final double MYTHICMULTIPLIER = 1.5;
    public static final double RAREMULTIPLIER = 1.3;
    public static final double UNCOMMONMULTIPLIER = 1.1;
    public static final double COMMONMULTIPLIER = 1.0;

    public static final double SHARDVALUE = .75;
    public static final double CMCVALUE = 2;
    public static final double COLORVALUE = .25;
    public static final double BASE = .5;

    /**
     * Description
     * @param card
     * @return value of card
     */
    @Override
    public final double evaluate (Card card) {
        double Cardvalue = (BASE + getStatChange(card) + getCMCValue(card) + getColorValue(card));
        double value = (Cardvalue) * getRareMultiplier(card);
        // If the card is in our hand, lower its value if we need to draw more lands to play it.
        if (card.isInZone(ZoneType.Hand) && card.getController().getLandsAvaliable() < card.getCMC()) {
            double temp1 = card.getController().getLandsAvaliable();
            double temp2 = card.getCMC();
            value = value*temp1/temp2;
        }
        return value;
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

    public double getCMCValue (Card card){
        return card.getCMC() * CMCVALUE;
    }

    public double getStatChange (Card card){
        return this.getPowerChange(card)
                + this.getToughnessChange(card);
    }

    public double getPowerChange (Card card){
        return card.getCurrentPower() - card.getBasePower();
    }


    public double getToughnessChange (Card card){
        return card.getCurrentToughness() - card.getBaseToughness();
    }

    /**
     This method determines the rarity of the card, and how powerful each rarity is.
     @param card - card object
     @return rareValue - the multiplier associated with the card's rarity.
     */
    public double getRareMultiplier (Card card){
        double rareValue = 0;
        switch(card.getRarity().toString()){
            case("L"):
                rareValue = COMMONMULTIPLIER;
                break;
            case("C"):
                rareValue = COMMONMULTIPLIER;
                break;
            case("U"):
                rareValue = UNCOMMONMULTIPLIER;
                break;
            case("R"):
                rareValue = RAREMULTIPLIER;
                break;
            case("M"):
                rareValue = MYTHICMULTIPLIER;
                break;
            default:
                if (rareValue == 0) {
                    System.err.println("Unexpected Rarity Found");
                }
                break;
        }
        return rareValue;
    }

    /**
     This method determines the number of colors in the card's casting cost.
     @param card - card object
     @return the number of colors
     */
    public int getNumColors (Card card){
        int colors = 0;
        for (ManaCostShard m : ManaCostShard.values()) {
            if (card.getManaCost().getShardCount(m) != 0 && m != ManaCostShard.GENERIC) {
                colors++;
            }
        }
        return colors;
    }

    /**
     This determines the number of colored mana symbols in the card's casting cost.
     @param card - card object
     @return the number of colored mana symbols in the card.
     */
    public int getShardCount (Card card){
        int shardcount = 0;
        for (ManaCostShard m : ManaCostShard.values()) {
            if (m != ManaCostShard.GENERIC) {
                shardcount += (card.getManaCost().getShardCount(m));
            }
        }
        return shardcount;
    }

    /**
     This totals the value of a card's colors and colored mana symbols in its cost.
     @param card - card object
     @return the total value of the card's color and devotion
     */
    public double getColorValue (Card card){
        return getNumColors(card) * COLORVALUE + getShardCount(card) * SHARDVALUE;
    }
}
