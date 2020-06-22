/**
 * Returns which card to play using a 'voting' system
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 22 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.player.Player;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;

import static forge.game.research.decision.strategy.DeckStrategies.*;

public class BallotBox {
    public Player controller;
    public ArrayList<SpellAbility> nonlands;
    public ArrayList<Card> lands;

    public BallotBox(Player p){
        controller = p;
    }

    public BallotBox(){}
    public void getOptions() {
        ViablePlays vp = new ViablePlays(controller);
        nonlands = vp.getNonlandPlays();
        lands = vp.getLandPlays();
    }
    public DoublyLinkedList<StrategyNode> getVotes(DeckStrategies deckstrategies){
        DoublyLinkedList<StrategyNode> votednodes = new DoublyLinkedList<StrategyNode>();
        for(Strategy strategy : monoredStrats.getStrategies()){
            //TODO: make this a general case/put in a specific passed in strategy
            votednodes.pushFront(getViableNode(strategy));
        }
        return votednodes;
    }

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return
     */
    public StrategyNode votedCard(DeckStrategies deckstrategies){
        HashMap<StrategyNode, Integer> votesofcards = new HashMap<StrategyNode, Integer>();
        for(StrategyNode node : getVotes(deckstrategies)){
            if(votesofcards.get(node).equals(null)){
                votesofcards.put(node, 0);
            }
            int tempvotes = votesofcards.get(node);
            votesofcards.replace(node, tempvotes+1);
        }

        int max = 0;
        StrategyNode votednode = new StrategyNode(null, null);
        for(StrategyNode node : votesofcards.keySet()){
            if(votesofcards.get(node) > max){
                max = votesofcards.get(node);
                votednode = new StrategyNode(node);
            }
        }

        return votednode;
    }


    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public StrategyNode getViableNode(Strategy strategy){
        ViablePlays vp = new ViablePlays(controller);
        nonlands = vp.getNonlandPlays();
        StrategyNode current = strategy.next();
        if (!current.isViable(nonlands, controller)){
            if (strategy.hasNext()) {
                current = strategy.next();
            } else {
                return null;
            }
        }
        return current;

    }
}
