package forge.game.research.Zone;

import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ExileEval extends ZoneEvaluator {
    //constants
    /**
     * BATTLEMUL - For comparing zones to other zones
     */
    public static final double EXILEEVAL = .1;

    /**
     * Constructor that calls the main constructor from its parent's class
     * @param p The player
     */
    public ExileEval(Player p) {
        super(ZoneType.Exile, p, EXILEEVAL);
    }

    /**
     * Evaluate the current state of the card and return a value based on that
     * @param c
     * @return double value of the card
     */
    @Override
    public double evaluateCard(Card c) {
        double count = 0;
        for (SpellAbility s: c.getSpellAbilities()) {
            if (s.getRestrictions().canPlay(c, s)) {
                count += 1;
                //s.getPayCosts().getTotalMana().getCMC()
            }
        }
        //Look for play from graveyard keywords
        if (c.hasKeyword(Keyword.SUSPEND) || c.hasKeyword(Keyword.REBOUND)) {
            count += 1;
        }

        double value = super.evaluateCard(c);
        if (count==0) {
            return value;
        } else {
            return value/EXILEEVAL;
        }
    }
}
