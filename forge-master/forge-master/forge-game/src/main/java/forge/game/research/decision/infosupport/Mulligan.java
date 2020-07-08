/**
 * Decides on when and how to mulligan
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 30 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.research.zone.DeckEval;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public class Mulligan {

    Player controller;

    //number of times Ai have mulled
    private int timeMull = 0;

    //number of times Ai should mull
    public static final int STOPMULL = 3;

    public static final int NONLANDSNEEDED = 2;
    public static final int STARTINGHANDSIZE = 7;

    public Mulligan () {
    }

    /**
     *
     * @param mullingPlayer
     * @param cardsNeeded
     * @return
     */
    public CardCollection returnCards(Player mullingPlayer, int cardsNeeded) {
        CardCollection returning = new CardCollection();
        CardCollection hand = new CardCollection();
        for (Card c: mullingPlayer.getZone(ZoneType.Hand)) {
            hand.add(c);
        }
        for (int count=0; count<cardsNeeded; count++) {
            //TODO: Find our worst card.
            returning.add(hand.remove(0));
        }
        return returning;
    }

    /**
     *
     * @param player
     * @return boolean
     */
    public boolean shouldMull (Player player) {
        //controls the max number of times Ai should mulligan
        if(timeMull == STOPMULL){return false;}

        timeMull++;
        
        int lands = 0;

        for(Card c : player.getCardsIn(ZoneType.Hand)){
            if(c.isLand()){lands++;}
        }
        if (doesNotHaveLands(player)) {
            return false;
        }
        DeckEval deckeval = new DeckEval(player);
        //take the average mana cost-1 and decide whether or not to mull if we dont have at least that many lands
        if(lands < deckeval.averageManaCost()-1 || lands > (STARTINGHANDSIZE - timeMull) - NONLANDSNEEDED){
            return true;
        }
        //should Ai mull?
        return false;
    }

    /**
     * Allows outside classes to get the number of times Ai has made
     * @return timeMull
     */
    public int getTimeMull() {
        return timeMull;
    }

    /**
     * Helper method that determines if a player's library does not have lands
     * @param player
     * @return
     */
    private boolean doesNotHaveLands(Player player) {
        for (Card card: player.getCardsIn(ZoneType.Library)) {
            if (card.isLand()) {
              return false;
            }
        }
        return true;
    }
}
