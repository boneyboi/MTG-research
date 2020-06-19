/**
 * Description
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy;

import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;

public class StrategyNode {

    public DoublyLinkedList<CardTemplate> requirements;
    public DoublyLinkedList<CardTemplate> cards;

    public StrategyNode(DoublyLinkedList<CardTemplate> requirements,
                        DoublyLinkedList<CardTemplate> cards){
        this.requirements = requirements;
        this.cards = cards;
    }

    public StrategyNode(StrategyNode node){
        this(node.requirements, node.cards);
    }

    public CardTemplate nextReq(){
        if(this.requirements.iterator().hasNext()){
            return this.requirements.iterator().next();
        } else {
            return null;
        }
    }
    public CardTemplate nextCard(){
        if(this.cards.iterator().hasNext()){
            return this.cards.iterator().next();
        } else {
            return null;
        }
    }

    public boolean isViable(ArrayList<SpellAbility> options) {
        ArrayList<SpellAbility> optionsleft = options;
        SpellAbility chosen = null;
        for (CardTemplate c: cards) {
            boolean found = false;
            for (SpellAbility spell: optionsleft) {
                if (c.matches(spell)) {
                    found = true;
                    chosen = spell;
                }
            }
             if (!found) {
                 return false;
             }
             optionsleft.remove(chosen);
        }
        return true;
    }
}
