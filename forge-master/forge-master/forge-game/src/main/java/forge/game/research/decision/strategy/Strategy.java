/**
 * Template for creating strategies
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy;

import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.template.CardTemplate;

import java.util.Iterator;

public class Strategy{

    private DoublyLinkedList<StrategyNode> path;
    private String name;
    private Iterator<StrategyNode> iter;

     //TODO: Add variables to let this create a strategy off of a file import.

    public Strategy(String title) {
        path = new DoublyLinkedList<StrategyNode>();
        pushFront(this, new StrategyNode(new DoublyLinkedList<CardTemplate>(),
                new DoublyLinkedList<CardTemplate>()));
        name = title;
        iter = path.iterator();
    }


    //create a new strategy node
    public void pushFront(Strategy strategy, StrategyNode node){
        strategy.path.push_front(new StrategyNode(node));
    }
    //TODO: remove these 2 functions
    //pushcard creates a new card in the strategy node
    public void pushCard(CardTemplate template){
        path.iterator().next().cards.push_front(template);
    }
    //pushreq creates a new requirement in the strategy node
    public void pushReq(CardTemplate template){
        iter.next().requirements.push_front(template);
    }


    public StrategyNode next(){
        if(iter.hasNext()){
            return iter.next();
        } else {
            return null;
        }
    }

    public boolean hasNext(){
        return iter.hasNext();
    }

    /**
     * checks if a node has it's requirements met
     * @return
     */
    public boolean allRequirementsInPlay(){
        return false;
    }


}
