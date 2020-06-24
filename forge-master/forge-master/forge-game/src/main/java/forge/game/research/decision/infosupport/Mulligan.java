/**
 * Decides on when and how to mulligan
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 24 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public class Mulligan {

    //number of times Ai have mulled
    private int timeMull = 0;

    //number of times Ai should mull
    public static final int STOPMULL = 0;

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
