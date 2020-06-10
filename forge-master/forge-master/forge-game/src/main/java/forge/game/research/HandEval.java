package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
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
    public double evaluateCard(Card card){

            Front frontC = new Front(card);
            double value = frontC.chooser();
            double cardCMC = card.getCMC();
            double landsHad = p.getLandsAvaliable();
            if (landsHad < cardCMC) {
                double multiplier = Math.pow(landsHad / cardCMC, 2);
                value = value * multiplier;
            }
            double count = .8;
            for (SpellAbility s: card.getSpellAbilities()) {
                if (s.getRestrictions().canPlay(card,s)) {
                    count+=.2;
                }
            }
            value = value*(count);
        return value;

    }

}