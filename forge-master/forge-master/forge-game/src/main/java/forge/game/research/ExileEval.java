package forge.game.research;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ExileEval extends ZoneEvaluator{

    public static final double EXILEMUL = 1;
    /**
     * Description
     * @param p The player
     */
    public ExileEval(Player p){
        super(ZoneType.Exile, p, EXILEMUL);
    }
}
