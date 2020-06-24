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
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.CardTemplate;
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
    public DoublyLinkedList<StrategyNode> getVotes(DeckStrategy deckstrategy){
        DoublyLinkedList<StrategyNode> votednodes = new DoublyLinkedList<StrategyNode>();
        for(Strategy strategy : deckstrategy.getStrategies()){
            //TODO: make this a general case/put in a specific passed in strategy
            votednodes.push_front(getViableNode(strategy));
        }
        return votednodes;
    }

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return
     */
    public SpellAbility votedCard(DeckStrategy deckStrategy) {
        getOptions();
        HashMap<SpellAbility, Integer> votesofcards = new HashMap<SpellAbility, Integer>();
        for (SpellAbility option : nonlands) {
            votesofcards.put(option, 0);
        }

        for (Strategy strat : deckStrategy.getStrategies()) {
            StrategyNode node = getViableNode(strat);
            //Find card from node
            if (node != null && node.getCards() != null) {
                for (CardTemplate template : node.getCards()) {
                    for (SpellAbility option : nonlands) {
                        if (template.matches(option)) {
                            int tempvotes = votesofcards.get(option);
                            votesofcards.replace(option, tempvotes+1);
                        }
                    }
                }
            }

        }

        int max = 0;
        SpellAbility chosen = null;
        for(SpellAbility option : votesofcards.keySet()){
            if(votesofcards.get(option) > max){
                max = votesofcards.get(option);
                if (option != null) {
                    chosen = option;
                }
            }
        }

        return chosen;



    }



            /**
            if(votesofcards.get(node)==null){
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
                if (node != null) {
                    votednode = new StrategyNode(node);
                }
            }
        }

        return votednode;
    }
             */




    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public StrategyNode getViableNode(Strategy strategy){
        StrategyNode current = new StrategyNode();
        while (current != null && !current.isViable(nonlands, controller)){
            if (strategy.hasNext()) {
                current = strategy.next();
            } else {
                strategy.next();
                return null;
            }
        }
        return current;

    }
}
