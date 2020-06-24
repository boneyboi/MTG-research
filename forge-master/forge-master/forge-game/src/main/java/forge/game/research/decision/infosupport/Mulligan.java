/**
 * Decides on when and how to mulligan
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 24 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class Mulligan {

    private int timeMull = 0;

    public static final int STOPMULL = 0;

    public Mulligan () {
    }


    public boolean testing (Player player) {
        //System.out.println(player.getCardsIn(ZoneType.Hand));
        boolean shouldAiMull = false;
        timeMull++;

        return shouldAiMull;
    }

    public int getTimeMull() {
        return timeMull;
    }
}
