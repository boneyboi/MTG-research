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

    public Strategy(String name) {
        path = new DoublyLinkedList<StrategyNode>();
        path.pushFront(new StrategyNode(new DoublyLinkedList<CardTemplate>(),
                new DoublyLinkedList<CardTemplate>()));
        this.name = name;
    }

    public void pushFront(StrategyNode node){
        path.pushFront(new StrategyNode(node));
    }
    //TODO: remove these 2 functions
    public void pushCard(CardTemplate template){
        path.iterator().next().cards.pushFront(template);
    }
    public void pushReq(CardTemplate template){
        path.iterator().next().requirements.pushFront(template);
    }

    public StrategyNode next(){
        if(path.iterator().hasNext()){
            return path.iterator().next();
        } else {
            return null;
        }
    }


    public boolean allRequirementsInPlay(){
        return false;
    }


}
