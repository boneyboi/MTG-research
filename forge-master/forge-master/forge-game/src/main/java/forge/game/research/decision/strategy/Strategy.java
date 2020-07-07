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
        pushFront(new StrategyNode(new DoublyLinkedList<CardTemplate>(),
                new DoublyLinkedList<CardTemplate>()));
        this.name = title;
        iter = path.iterator();
    }

    public void reset(){
        iter = path.iterator();
    }


    //create a new strategy node
    public void pushFront(StrategyNode node){
        this.path.push_front(new StrategyNode(node));
    }
    //TODO: remove these 2 functions
    //pushcard creates a new card in the strategy node
    public void pushCard(int index, CardTemplate template){
        for(int i = 0; i < index; i++){
            if(this.path.iterator().hasNext()){
                this.path.iterator().next();
            }
        }
        if(this.path.iterator().hasNext()){
            ///!!!!!!!!!!!!!!!!this might be where the error of replacing the node with a new card comes from
            this.path.iterator().next().cards.push_front(template);
        } else {
            System.out.println("There was no next strategynode");
        }
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

    public StrategyNode get(int index){
        for(int i = 0; i < index; i++){
            if(iter.hasNext()){
                iter.next();
            } else {
                return null;
            }
        }
        return iter.next();
    }
    /**
     * checks if a node has it's requirements met
     * @return
     */
    public boolean allRequirementsInPlay(){
        return false;
    }

    public String getName(){
        return this.name;
    }
}
