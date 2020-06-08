package forge.game.research;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;

//*Evaluator
public class evaluateValue{
    //replace numbers with constant variables
    public static final double evalVal(Card card){

        double shardmana = 0.0;
        for(ManaCostShard m : ManaCostShard.values()){
            if(m!=ManaCostShard.GENERIC) {
                shardmana += (card.getManaCost().getShardCount(m) * 0.75);
                if (card.getManaCost().getShardCount(m) != 0) {
                    shardmana += ((card.getManaCost().getShardCount(m) / card.getManaCost().getShardCount(m)) * 0.25);
                }
            }
        }
        return 0.5 + (card.getCurrentPower() - card.getBasePower())
                + (card.getCurrentToughness() - card.getBaseToughness())
                + (card.getCMC()*2) + shardmana;
    }
    //add in change in toughness/power calculator
}