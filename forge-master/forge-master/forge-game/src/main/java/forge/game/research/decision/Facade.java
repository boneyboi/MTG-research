/**
 * Class that returns the decision when the computer asks for one
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 17 2020
 */
package forge.game.research.decision;

import java.util.ArrayList;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.infosupport.BallotBox;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.CardTemplate;

import static forge.game.ability.AbilityKey.Player;

public class Facade {

    public Player controller;

    public Facade(Player p) {
        controller = p;
    }

    /**
     * Used when deciding attackers
     * @param possAttackers : list
     * @param possBlockers : list
     * @return list of creatures
     * TODO: change void to 'list or arraylist', fill out body of method, edit docstring if necessary
     */
    public void getAttackers (ArrayList possAttackers, ArrayList possBlockers) {
    }
    public void getAttackers (ArrayList possAttackers, ArrayList possBlockers,
                              ArrayList player1hand, ArrayList player2hand) {
    }
    /**
     * Used when deciding blockers
     * @param possAttackers : list
     * @param possBlockers : list
     * @return map
     * TODO: change void to 'map', create map, fill out body of method, edit docstring if necessary
     */
    public void getBlockers (ArrayList possAttackers, ArrayList possBlockers) {
    }
    public void getBlockers (ArrayList possAttackers, ArrayList possBlockers,
                             ArrayList player1hand, ArrayList player2hand) {
    }
    /**
     * Used when our creature is blocked by multiple creatures, and we have to assign damage.
     * @param attackPow
     * @param defenders
     * @return damageAssigned
     * TODO: void to list or arraylist, fill out body
     */
    public int multiBlock (int attackPow, ArrayList defenders) {
        int damageAssigned = 0;

        return damageAssigned;
    }


    /**
     * Used when deciding what card or ability to play
     * @param: options : list
     * @return a card to play
      *TODO: fill out body, and have it return a card
     */
     public DoublyLinkedList<CardTemplate> getNextPlayDecision (DeckStrategies deckstrategy) {
         //call the decision maker from the other class and pass the card it voted on through here
         BallotBox b = new BallotBox();
         //go through the list of card templates and find the exact cards we should play from our hand
         return b.votedCard(deckstrategy).cards;
     }

    /**
     * Play a card to the field
     * @param deckstrategy
     */
    public void playStrategy(StrategyNode strategynode){
        //use ballot box to see what it voted on
        //then play the card
        DoublyLinkedList<Card> cards = new DoublyLinkedList<Card>();
        while(strategynode.cards.iterator().hasNext()){
            //convert each template to an actual card
            //then play that card
        }
        //playCards(getNextPlayDecision(strategynode));
    }

    /**
     *
     * @param card
     */
    public void playCards(DoublyLinkedList<Card> cards){
        //play the cards

        while(cards.iterator().hasNext()){
            playCard(cards.iterator().next());
        }
    }

    public void playCard(Card card){

    }

     /**
     * Used when deciding to mulligan and what cards to keep if we do.
     * @param mullAlready
      * @oaram
     * @return List of cards to put back, or null if we want to mulligan
      * TODO: decide on return, create 'hand' parameter somehow, fill out body
     */
     public void mulligan(int mullAlready) {
     }

     /**
     * Used when we have to discard a card but can choose which card to discard.
     * @param
     * @return discardCard
      * TODO: fill out body, find way to pass hand
      */
     public Card forcedDiscard () {
         Card discardCard = null;

         return discardCard;
     }

     /**
     * Used when we are forced to sacrifice a permanent.
     * @param canBeSacrificed
     * @return sacrificeCard
      * TODO: fill out body
     */
     public Card forcedSacrifice (ArrayList canBeSacrificed) {
         Card sacrificeCard = null;

         return sacrificeCard;
     }


}
