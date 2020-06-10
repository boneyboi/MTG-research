package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class HandEval extends ZoneEvaluator {

    public HandEval(Player p) {
        super(ZoneType.Hand, p, 1);
    }

    @Override
    public double evaluateZone(){
        double result = 0;
        for(Card c: p.getCardsIn(zone)){
            double value = evaluateCard(c);
            double cardCMC = c.getCMC();
            double landsHad = p.getLandsAvaliable();

            if (landsHad < cardCMC) {
                 value *= Math.pow(landsHad/cardCMC, 2);
            }

            value = value*multiplier;
            result += value;
        }
        return result;
    }

}