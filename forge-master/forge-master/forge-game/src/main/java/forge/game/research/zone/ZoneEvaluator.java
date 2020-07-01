/**
 * Abstract class that evaluates the 'zone' i.e. hand, battlefield, exile, graveyard, library
 * by given each zone a value, which is the sum of the values of the cards in their respective
 * zones
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.zone;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.card.Front;
import forge.game.zone.ZoneType;

public abstract class ZoneEvaluator {

    protected static ZoneType zone;
    protected static Player p;

    /**
     *
     * @param zone
     * @param p: player allows to access the cards within zones of the respective player
     */
    public ZoneEvaluator(ZoneType zone, Player p){
        this.zone = zone;
        this.p = p;
    }

    /**
     * Evaluate the value of a specific card
     * @param c
     * @return double result: value of the card
     */
    public double evaluateCard(Card c){
        double result = 0;
        //evaluate card * this.multiplier;
        Front frontC = new Front();
        double value = frontC.chooser(c);
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

    public Card getHighestValueIn() {
        double max = 0;
        Card highest = null;
        //EvaluateCard for all cards in the zone
        for(Card c: p.getCardsIn(zone)){
            if (evaluateCard(c) > max) {
                highest = c;
                max = evaluateCard(c);
            }
        }
        return highest;
    }
}
