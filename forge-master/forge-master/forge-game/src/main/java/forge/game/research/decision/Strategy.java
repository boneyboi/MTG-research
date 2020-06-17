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

    public Strategy(DoublyLinkedList<Card> requirements, DoublyLinkedList<Card> cards, String name) {
        path = new DoublyLinkedList<StrategyNode>();
        this.requirements = requirements;
        this.cards = cards;
        this.name = name;
        path.pushFront(new StrategyNode());
    }

    public void CheckRequirements(){

    }

}
