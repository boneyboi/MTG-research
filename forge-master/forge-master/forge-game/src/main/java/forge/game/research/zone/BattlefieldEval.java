/**
 * (For Research) Description later
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 08, 2020
 */

package forge.game.research.zone;

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

    @Override
    public double evaluateZone(){
        double value = super.evaluateZone();
        return value*BATTLEMUL;
    }
}
