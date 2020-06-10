package forge.game.research;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class BattlefieldEval extends ZoneEvaluator {

    //constants
    /**
     * BATTLEMUL - For comparing zones to other zones
     */
    public static final double BATTLEMUL = 1;

    /**
     * Constructor that calls the main constructor from its parent's class
     * @param p The player
     */
    public BattlefieldEval(Player p) {
        super(ZoneType.Battlefield, p, BATTLEMUL);
    }

}
