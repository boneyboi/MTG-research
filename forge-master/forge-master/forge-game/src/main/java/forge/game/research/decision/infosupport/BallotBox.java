/**
 * Returns which card to play using a 'voting' system
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;

public class BallotBox {

    public BallotBox(){}

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return
     */
    public Card votedCard(){
        return null;
    }

    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public StrategyNode getViableNode(Strategy strategy){

        return null;
    }
}
