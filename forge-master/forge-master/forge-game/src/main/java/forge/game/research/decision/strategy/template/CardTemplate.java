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

public abstract class CardTemplate{


    /**
     * A template for what kinds of cards are played in the strategy class
     */
    public CardTemplate() {
    }

    /**
     * Returns whether or not the card matches the template
     * @param card
     * @return boolean
     */
    public boolean matches(Card card){
        return false;
    }
}
