package forge.game.research.zone;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StackEval extends ZoneEvaluator{
//TODO: How do we want to value the stack?
    public StackEval(Player p) {
        super(ZoneType.Stack, p);
    }


}
