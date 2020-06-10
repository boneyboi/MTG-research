package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class BattlefieldEval extends ZoneEvaluator {

    public BattlefieldEval(Player p) {
        super(ZoneType.Battlefield, p);
    }

    public double evaluateZone(){
        double result = 0;
        for(Card c: p.getCardsIn(zone)){
            Front frontC = new Front(c);
            result += frontC.chooser();
        }
        return result;
    }
}
