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

    public double evaluateZone(){
        double result = 0;
        for(Card c: p.getCardsIn(zone)){
            Front frontC = new Front(c);
            result += frontC.chooser();
        }
        System.out.println("Total: " + result);
        return result;
    }
}
