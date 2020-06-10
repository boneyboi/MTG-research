package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public abstract class ZoneEvaluator {

    protected static ZoneType zone;
    protected static Player p;
    protected static double multiplier;

    public ZoneEvaluator(ZoneType zone, Player p, double multiplier){
        this.zone = zone;
        this.p = p;
        this.multiplier = multiplier;
    }

    public double evaluateCard(){
        //evaluate card * this.multiplier;
        return 0;
    }

    public double evaluateZone(){
        double result = 0;
        //EvaluateCard for all cards in the zone
        for(Card c: p.getCardsIn(zone)){
            Front frontC = new Front(c);
            result += frontC.chooser();
        }
        return result;
    }
}
