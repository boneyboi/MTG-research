/**
 * Description
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision;

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
