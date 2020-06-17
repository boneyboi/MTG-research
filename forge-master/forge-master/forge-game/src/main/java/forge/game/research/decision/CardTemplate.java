package forge.game.research.decision;

import forge.game.Game;
import forge.game.card.Card;

public abstract class CardTemplate extends Card {



    /**
     * A template for cards to be played in the strategy class
     * @param id0
     * @param game0
     */
    public CardTemplate(int id0, Game game0) {
        super(id0, game0);
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
