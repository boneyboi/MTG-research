package forge.game.research.decision;

import forge.game.Game;
import forge.game.card.Card;

public abstract class CardTemplate{


    /**
     * A template for cards to be played in the strategy class
     */
    public CardTemplate() {
    }

    /**
     * Returns whether or not the card matches the template
     * @param card
     * @return
     */
    public boolean matches(Card card){
        return false;
    }
}
