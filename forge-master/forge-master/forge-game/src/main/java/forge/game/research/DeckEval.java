package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DeckEval extends ZoneEvaluator{

    public DeckEval(Player p) {
        super(ZoneType.Library, p);
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
