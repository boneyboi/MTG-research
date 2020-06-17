package forge.game.research.decision;

import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;

public class StrategyNode {

    public DoublyLinkedList<CardTemplate> requirements;
    public DoublyLinkedList<CardTemplate> cards;

    public StrategyNode(DoublyLinkedList<CardTemplate> requirements,
                        DoublyLinkedList<CardTemplate> cards){
        this.requirements = requirements;
        this.cards = cards;
    }

    public StrategyNode(StrategyNode node){
        this.requirements = node.requirements;
        this.cards = node.cards;
    }
}
