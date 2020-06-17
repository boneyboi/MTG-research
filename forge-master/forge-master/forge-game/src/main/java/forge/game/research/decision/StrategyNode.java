package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;

public class StrategyNode {

    private DoublyLinkedList<CardTemplate> requirements;
    private DoublyLinkedList<CardTemplate> cards;

    public StrategyNode(StrategyNode node){
        this.requirements = node.requirements;
        this.cards = node.cards;
    }
}
