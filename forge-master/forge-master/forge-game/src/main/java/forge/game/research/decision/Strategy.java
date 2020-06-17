package forge.game.research.decision;

import forge.game.ability.effects.DetachedCardEffect;
import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;

public class Strategy{

    private DoublyLinkedList<StrategyNode> path;
    private DoublyLinkedList<Card> requirements;
    private DoublyLinkedList<Card> cards;
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
    public void pushCard(CardTemplate template){
        path.iterator().next().cards.pushFront(template);
    }
    public void pushReq(CardTemplate template){
        path.iterator().next().requirements.pushFront(template);
    }

    public StrategyNode next(){
        return path.iterator().next();
    }



    public boolean CheckRequirements(){
        return false;
    }
}
