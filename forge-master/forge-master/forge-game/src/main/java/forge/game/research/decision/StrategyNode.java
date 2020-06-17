package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;

public class StrategyNode {

    private DoublyLinkedList<Card> requirements;
    private DoublyLinkedList<Card> cards;

    public StrategyNode(){
        DoublyLinkedList<Card> requirements = new DoublyLinkedList<Card>();
        DoublyLinkedList<Card> cards = new DoublyLinkedList<Card>();
    }
}
