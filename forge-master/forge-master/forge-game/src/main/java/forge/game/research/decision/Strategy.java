package forge.game.research.decision;

import forge.game.ability.effects.DetachedCardEffect;
import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;

public class Strategy{

    private DoublyLinkedList<DoublyLinkedList> path;


     //TODO: Add variables to let this create a strategy off of a file import.

    public Strategy() {
        path = new DoublyLinkedList<DoublyLinkedList>();
        path.pushFront(new DoublyLinkedList<Card>());

    }
}
