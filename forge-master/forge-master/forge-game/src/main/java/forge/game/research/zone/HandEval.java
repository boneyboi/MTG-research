/**
 * Subclass for ZoneEvaluator -- gives a value to the zone 'library'
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 12, 2020
 */

package forge.game.research.zone;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.card.Front;
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
        super(ZoneType.Hand, p);
    }

    /**
     * Evaluate's the value of a card in hand
     * @return double: value of card
     */
    @Override
    public double evaluateCard(Card card){

            Front frontC = new Front();
            double value = frontC.chooser(card);
            double cardCMC = card.getCMC();
            double landsHad = p.getLandsAvaliable();
            //TODO: Account for if we have the right colors
            if (landsHad < cardCMC) {
                double multiplier = Math.pow(landsHad / cardCMC, 2);
                value = value * multiplier;
            }

            //Accounts for the multiple ways you can play certain cards.
            if (!card.isLand()) {
                double count = .8;
                for (SpellAbility s : card.getSpellAbilities()) {
                    if (s.getRestrictions().canPlay(card, s)) {
                        count += .2;
                        //s.getPayCosts().getTotalMana().getCMC()
                    }
                }
                value = value * (count);
            }
        return value;

    }

    @Override
    public double evaluateZone(){
        double value = super.evaluateZone();
        return value*HANDMUL;
    }

}