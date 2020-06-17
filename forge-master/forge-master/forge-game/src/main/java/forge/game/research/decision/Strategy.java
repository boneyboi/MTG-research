package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;

public class Strategy{

    private DoublyLinkedList<Card> path;


     //TODO: Add variables to let this create a strategy off of a file import.

    public Strategy() {
        path = new DoublyLinkedList<Card>();
    }
}
