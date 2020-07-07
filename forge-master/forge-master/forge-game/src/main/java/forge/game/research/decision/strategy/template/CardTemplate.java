/**
 * Abstract class that allow for the creation of templates that can be used to filter based on:
 * CMC, Name
 * Lifebuff, Lifegain
 * Surveil, SurveilBoost
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public abstract class CardTemplate{

    private String name;
    /**
     * A template for what kinds of cards are played in the strategy class
     */
    public CardTemplate() {
        this.name = "Card Template";
    }

    /**
     * Returns whether or not the card matches the template
     * @param sa
     * @return boolean
     */
    public boolean matches(SpellAbility sa){
        return false;
    }

    public boolean matches(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (matches(sa)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString(){
        return this.getClass().toString();
    }
}
