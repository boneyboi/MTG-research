/**
 * Abstract class that allows for card's to be evaluated (given a value) based on their 'type'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 23, 2020
 */
package forge.game.research.card;


import forge.card.mana.ManaCostShard;
import forge.game.card.Card;


public abstract class CardEvaluator {
    //constants - first four are rarity
    public static final double MYTHICMULTIPLIER = 1.5;
    public static final double RAREMULTIPLIER = 1.3;
    public static final double UNCOMMONMULTIPLIER = 1.1;
    public static final double COMMONMULTIPLIER = 1.0;

    /**
     * SHARDVALUE - value of individual mana symbols (this is the same for generic and colored)
     * CMCVALUE - converted mana cost's value
     * COLORVALUE - how much colored mana symbols is valued in comparison to generic mana
     */
    public static final double SHARDVALUE = .75;
    public static final double CMCVALUE = 2;
    public static final double COLORVALUE = .25;

    public CardEvaluator() {
    }

    /**
     * A card's value depends on its:
     *  rarity
     *  number of colors
     *  number of colored mana symbols in its cost
     *  CMC
     * @param card - card object
     * @return the value of the card
     */
    public double evaluate(Card card) {
        double cardValue = (getCMCValue(card) + getColorValue(card));
        double totalValue = (cardValue) * getRareMultiplier(card);
        return totalValue;
    }

    public double getCMCValue (Card card){

        //in case a card has copied another card
        if (card.getCopiedPermanent() != null) {
            return card.getPaperCard().getRules().getMainPart().getManaCost().getCMC() * CMCVALUE;
        } else {
            return card.getCMC() * CMCVALUE;
        }
    }

    /**
     This method determines the rarity of the card, and how powerful each rarity is.
     @param card - card object
     @return rareValue - the multiplier associated with the card's rarity.
     */
    public double getRareMultiplier (Card card){
        double rareValue = 1;

        //sets multiplier depending on the rarity of the card
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
                if (rareValue == 1) {
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

        //iterates through every mana shard
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

        //number of colors * COLORVALUE + how many colored mana symbols does a card have * SHARDVALUE
        return (getNumColors(card) * COLORVALUE )+( getShardCount(card) * SHARDVALUE);
    }

}