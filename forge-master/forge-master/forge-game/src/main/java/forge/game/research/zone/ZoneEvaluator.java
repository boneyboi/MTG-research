/**
 * Description
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 10 2020
 */

package forge.game.research.zone;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.card.Front;
import forge.game.zone.ZoneType;

public abstract class ZoneEvaluator {

    protected static ZoneType zone;
    protected static Player p;
    protected static double multiplier; //TODO: remove from here and from constructer

    /**
     *
     * @param zone
     * @param p: player allows to access the cards within zones of the respective player
     * @param multiplier
     */
    public ZoneEvaluator(ZoneType zone, Player p, double multiplier){
        this.zone = zone;
        this.p = p;
        this.multiplier = multiplier;
    }

    /**
     * Evaluate the value of a specific card
     * @param c
     * @return double result: value of the card
     */
    public double evaluateCard(Card c){
        double result = 0;
        //evaluate card * this.multiplier;
        Front frontC = new Front(c);
        double value = frontC.chooser();
        result += value;

        return result;
    }

    /**
     * Evaluate the total value of a zone by summing the individual cards
     * @return double result: value of the zone
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
