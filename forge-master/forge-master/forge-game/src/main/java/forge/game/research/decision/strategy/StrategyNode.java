/**
 * TODO: Description
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 23 June 2020
 */

package forge.game.research.decision.strategy;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class StrategyNode {

    public DoublyLinkedList<CardTemplate> requirements;
    public DoublyLinkedList<CardTemplate> cards;
    public boolean finished = false;
    public boolean repeatable = false;

    public StrategyNode(){
        this(true);
    }

    public StrategyNode(boolean repeat) {
        this.requirements = new DoublyLinkedList<CardTemplate>();
        this.cards = new DoublyLinkedList<CardTemplate>();
        repeatable = repeat;
    }

    /**
     * Constructs a StrategyNode that has requirements and cards
     * @param requirements
     * @param cards
     */
    public StrategyNode(DoublyLinkedList<CardTemplate> requirements,
                        DoublyLinkedList<CardTemplate> cards, boolean repeated){
        this.requirements = requirements;
        this.cards = cards;
        repeatable = repeated;
    }

    public StrategyNode(DoublyLinkedList<CardTemplate> requirements,
                        DoublyLinkedList<CardTemplate> cards) {
        this.requirements = requirements;
        this.cards = cards;
        repeatable = false;
    }

    /**
     * TODO: Description
     * @param node
     */
    public StrategyNode(StrategyNode node){
        this(node.requirements, node.cards, node.repeatable);
    }

    /**
     * Gets the next requirement of a node
     * @return the next requirement of a node
     * @return null (if there is no next requirement)
     */
    public CardTemplate nextReq(){
        if(this.requirements.iterator().hasNext()){
            return this.requirements.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * Gets the next card
     * @return the next card
     * @return null (description what null means in this case)
     */
    public CardTemplate nextCard(){
        if(this.cards.iterator().hasNext()){
            return this.cards.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * If a node is repeatable, 'repeatable' is set to true
     */
    public void markRepeatable() {
        this.repeatable = true;
    }

    /**
     * Whether or not requriments have been met for a node
     * @param p - player
     * @return boolean true (description what true means) or false (description what false means)
     */
    public boolean reqsDone(Player p) {
        for (CardTemplate req: requirements) {
            boolean found = false;
            for (Card c : p.getZone(ZoneType.Battlefield)) {
                for (SpellAbility sa: c.getSpellAbilities()) {
                    if (!found && req.matches(sa)) {
                        found = true;
                    }

                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if a node has already been completed, if a node is repeatable this is false always
     * @param p - player
     * @return boolean true (this node has been done) or false (this node has not been done)
     */
    public boolean alreadyDone(Player p) {
        if (repeatable) {
            return false;
        }
        int count = 0;
        for (CardTemplate c: cards) {
            boolean found = false;
            for (Card card : p.getZone(ZoneType.Battlefield)) {
                for (SpellAbility sa: card.getSpellAbilities()) {
                    if (!found && c.matches(sa)) {
                        found = true;
                    }

                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * TODO: Description
     * @return boolean true TODO: (description what true means) or false (description what false means)
     */
    public DoublyLinkedList<CardTemplate> getCards() {
        return cards;
    }

    /**
     * TODO: Description
     * TODO: @param  options
     * @param controller - the player who has priority
     * @return boolean true TODO: (description what true means) or false (description what false means)
     */
    public boolean isViable(ArrayList<SpellAbility> options, Player controller) {
        if (cards.isEmpty() || options.isEmpty()) {
            return false;
        }
        if (!reqsDone(controller) || alreadyDone(controller)) {
            return false;
        }
        ArrayList<SpellAbility> optionsUsed = new ArrayList<SpellAbility>();
        SpellAbility chosen = null;
        for (CardTemplate c: cards) {
            boolean found = false;
            for (SpellAbility spell: options) {
                if (c.matches(spell) && !optionsUsed.contains(spell)) {
                    found = true;
                    chosen = spell;
                }
            }
             if (!found) {
                 return false;
             }
             optionsUsed.add(chosen);
        }
        return true;
    }
}
