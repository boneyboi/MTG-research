package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public abstract class ZoneEvaluator {

    protected static ZoneType zone;
    protected static Player p;

    public ZoneEvaluator(ZoneType zone, Player p){
        this.zone = zone;
        this.p = p;
    }

    public abstract double evaluateZone();
}
