package forge.game.research.Zone;

import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class GraveyardEval extends ZoneEvaluator {
    //constants
    /**
     * GRAVEEVAL - For comparing zones to other zones
     */
    public static final double GRAVEEVAL = .1;

    /**
     * Constructor that calls the main constructor from its parent's class
     * @param p The player
     */
    public GraveyardEval(Player p) {
        super(ZoneType.Graveyard, p, GRAVEEVAL);
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
        if (c.hasKeyword(Keyword.FLASHBACK) || c.hasKeyword(Keyword.ETERNALIZE)
        || c.hasKeyword(Keyword.EMBALM) || c.hasKeyword(Keyword.RETRACE) ||
                c.hasKeyword(Keyword.ESCAPE) || c.hasKeyword(Keyword.AFTERMATH)) {
            count += 1;
        }

        double value = super.evaluateCard(c);

        if (count==0) {
            value = value;
        } else {
            value = value/GRAVEEVAL;
        }
        //higher the value more lands you have
        return value;
    }
}