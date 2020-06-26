/**
 * Decides on when and how to mulligan
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 24 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public class Mulligan {

    Player controller;

    //number of times Ai have mulled
    private int timeMull = 0;

    //number of times Ai should mull
    public static final int STOPMULL = 1;

    public Mulligan () {
    }

    /**
     *
     * @param hand
     */
    public void mulliganDecision(Zone hand){
        //player get cards in hand
        //mulligan if nonlnads > 65% of the hand or lands are
        //do forced discard from facade if we did mulligan(the cards still go to the library)

    }

    public CardCollection returnCards(Player mullingPlayer, int cardsNeeded) {
        CardCollection returning = new CardCollection();
        CardCollection hand = new CardCollection();
        for (Card c: controller.getZone(ZoneType.Hand)) {
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
     * @return
     */
    public boolean shouldMull (Player player) {
        timeMull++;

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
}
