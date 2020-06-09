package forge.game.research; /**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 09, 2020
 */
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;

public class EnchantmentEval extends CardEvaluator {
    public static final double MYTHICMULTIPLIER = 1.5;
    public static final double RAREMULTIPLIER = 1.3;
    public static final double UNCOMMONMULTIPLIER = 1.1;
    public static final double COMMONMULTIPLIER = 1.0;

    public static final double SHARDVALUE = .75;
    public static final double CMCVALUE = 2;
    public static final double COLORVALUE = .25;
    public static final double BASE = .5;

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
    public final double evaluate(Card card) {
        //TODO: Add an evaluation statement
        return 0.0;
    }

    public double getCMCValue (Card card){
        return card.getCMC() * CMCVALUE;
    }

    /**
     This method determines the rarity of the card, and how powerful each rarity is.
     @param card - card object
     @return rareValue - the multiplier associated with the card's rarity.
     */
    public double getRareMultiplier (Card card){
        double rareValue = COMMONMULTIPLIER;
        switch(card.getRarity().toString()){
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
                System.err.println("Unexpected Rarity Found");
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