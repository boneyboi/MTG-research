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

public class Strategy{

    private DoublyLinkedList<StrategyNode> path;
    private String name;

     //TODO: Add variables to let this create a strategy off of a file import.

    public Strategy(String title) {
        path = new DoublyLinkedList<StrategyNode>();
        pushFront(this, new StrategyNode(new DoublyLinkedList<CardTemplate>(),
                new DoublyLinkedList<CardTemplate>()));
        name = title;
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
        path.iterator().next().requirements.push_front(template);
    }


    public StrategyNode next(){
        if(path.iterator().hasNext()){
            return path.iterator().next();
        } else {
            return null;
        }
    }

    public boolean hasNext(){
        return path.iterator().hasNext();
    }


    public boolean allRequirementsInPlay(){
        return false;
    }


}
