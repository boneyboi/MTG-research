/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 10 2020
 */

package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public abstract class ZoneEvaluator {

    protected static ZoneType zone;
    protected static Player p;
    protected static double multiplier;

    /**
     *
     * @param zone
     * @param p
     * @param multiplier
     */
    public ZoneEvaluator(ZoneType zone, Player p, double multiplier){
        this.zone = zone;
        this.p = p;
        this.multiplier = multiplier;
    }

    /**
     *
     * @param c
     * @return the value of an individual card
     */
    public double evaluateCard(Card c){
        double result = 0;
        //evaluate card * this.multiplier;
        Front frontC = new Front(c);
        result += frontC.chooser();

        return result;
    }

    /**
     * Description
     * @return the value of the zone
     */
    public double evaluateZone(){
        double result = 0;
        //EvaluateCard for all cards in the zone
        for(Card c: p.getCardsIn(zone)){
            result += evaluateCard(c);
        }
        return result;
    }
}
