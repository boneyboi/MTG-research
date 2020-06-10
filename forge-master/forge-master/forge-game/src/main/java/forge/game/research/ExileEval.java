package forge.game.research;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ExileEval extends ZoneEvaluator{
    public ExileEval(Player p){
        super(ZoneType.Exile, p, 1);
    }
}
