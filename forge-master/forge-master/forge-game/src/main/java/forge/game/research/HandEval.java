package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class HandEval extends ZoneEvaluator {

    //constants
    /**
     * HANDMUL = For comparing zones to other zones
     */
    public static final double HANDMUL = 1;

    public HandEval(Player p) {
        super(ZoneType.Hand, p, HANDMUL);
    }

    /**
     *
     * @return
     */
    @Override
    public double evaluateZone(){
        double result = 0;
        for(Card c: p.getCardsIn(zone)){
            Front frontC = new Front(c);
            double value = frontC.chooser();
            double cardCMC = c.getCMC();
            double landsHad = p.getLandsAvaliable();
            if (landsHad < cardCMC) {
                double multiplier = Math.pow(landsHad/cardCMC, 2);
                value = value*multiplier;
            }
            result += value;
        }
        return result;
    }

}