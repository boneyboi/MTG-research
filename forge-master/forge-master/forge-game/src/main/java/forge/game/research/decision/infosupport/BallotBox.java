/**
 * Returns which card to play using a 'voting' system
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;

import java.util.Dictionary;
import java.util.Enumeration;

public class BallotBox {


    public BallotBox(){}

    public DoublyLinkedList<Card> getVotes(DeckStrategies deckstrategies){
        DoublyLinkedList<Card> votednodes = new DoublyLinkedList<Card>();
        for(Strategy strategy : deckstrategies.monoredStrats){
            votednodes.pushFront(getViableNode(strategy));
        }
        return votednodes;
    }

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return
     */
    public Card votedCard(DeckStrategies deckstrategies){
        Dictionary<String, Integer> votesofcards;
        /*for(StrategyNode node : getVotes(deckstrategies)){
            votesofcards.put(node.nextCard().)
        }*/
        return null;
    }

    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public Card getViableNode(Strategy strategy){

        return null;
    }
}
