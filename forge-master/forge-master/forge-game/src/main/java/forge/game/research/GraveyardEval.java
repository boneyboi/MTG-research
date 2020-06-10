package forge.game.research;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class GraveyardEval extends ZoneEvaluator {
    //constants
    /**
     * BATTLEMUL - For comparing zones to other zones
     */
    public static final double GRAVEEVAL = 1;

    /**
     * Constructor that calls the main constructor from its parent's class
     * @param p The player
     */
    public GraveyardEval(Player p) {
        super(ZoneType.Graveyard, p, GRAVEEVAL);
    }
}
