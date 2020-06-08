package forge.game.research;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;

public class CardEvaluator {
    public static final double MYTHICMULTIPLIER = 1.5;
    public static final double RAREMULTIPLIER = 1.3;
    public static final double UNCOMMONMULTIPLIER = 1.1;
    public static final double SHARDVALUE = .75;
    public static final double COMMONMULTIPLIER = 1.0;
    public static final double CMCVALUE = 2;
    public static final double COLORVALUE = .25;
    public static final double BASEVALUE = .5;

    /**
     * A card's value depends on its rarity, number of colors, number of colored mana symbols in its cost,
     * it's CMC and a base value.
     *
     * @param return the value of the card
     */
    public CardEvaluator() {

    }

    public final double evaluate (Card card){
        double value = (BASEVALUE + getStatChange(card)
                + getCMCValue(card)
                + getColorValue(card));
        return (value) * getRareMultiplier(card);
    }

    private double getCMCValue (Card card){
        return card.getCMC() * CMCVALUE;
    }

    private double getStatChange (Card card){
        return this.getPowerChange(card) + this.getToughnessChange(card);
    }

    private double getPowerChange (Card card){
        return card.getCurrentPower() - card.getBasePower();
    }


    private double getToughnessChange (Card card){
        return card.getCurrentToughness() - card.getBaseToughness();
    }

    /**
     This method determines the rarity of the card, and how powerful each rarity is.
     @param return the multiplier associated with the card's rarity.
     */
    private double getRareMultiplier (Card card){
        double rareValue = COMMONMULTIPLIER;
        if (card.getRarity().toString().equals("U")) {
            rareValue = UNCOMMONMULTIPLIER;
        } else if (card.getRarity().toString().equals("R")) {
            rareValue = RAREMULTIPLIER;
        } else if (card.getRarity().toString().equals("M")) {
            rareValue = MYTHICMULTIPLIER;
        }
        return rareValue;
    }

    /**
     This method determines the number of colors in the card's casting cost.
     @param return the number of colors
     */
    private double getNumColors (Card card){
        double colors = 0.0;
        for (ManaCostShard m : ManaCostShard.values()) {
            if (card.getManaCost().getShardCount(m) != 0 && m != ManaCostShard.GENERIC) {
                colors += (1);
            }
        }
        return colors;
    }

    /**
     This determines the number of colored mana symbols in the card's casting cost.
     @param return the number of colored mana symbols in the card.
     */
    private double getShardCount (Card card){
        double shardcount = 0.0;
        for (ManaCostShard m : ManaCostShard.values()) {
            if (m != ManaCostShard.GENERIC) {
                shardcount += (card.getManaCost().getShardCount(m));
            }
        }
        return shardcount;
    }

    private double getColorValue (Card card){
        return getNumColors(card) * COLORVALUE + getShardCount(card) * SHARDVALUE;
    }
}