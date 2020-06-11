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

    /**
     * Description
     * @param p the player
     */
    public HandEval(Player p) {
        super(ZoneType.Hand, p, HANDMUL);
    }

    /**
     * Evaluate's the value of a card in hand
     * @return double: value of card
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

            //Accounts for the multiple ways you can play certain cards.
            double count = .8;
            for (SpellAbility s: card.getSpellAbilities()) {
                if (s.getRestrictions().canPlay(card,s)) {
                    count+=.2;
                    //s.getPayCosts().getTotalMana().getCMC()
                }
            }
            value = value*(count);
        return value;

    }

}