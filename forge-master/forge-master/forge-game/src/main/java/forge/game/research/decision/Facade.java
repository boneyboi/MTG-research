/**
 * Class that returns the decision when the computer asks for one
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 23, 2020
 */
package forge.game.research.decision;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.cost.CostPart;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.research.DoublyLinkedList;
import forge.game.research.PlayCards;
import forge.game.research.card.CardEvaluator;
import forge.game.research.card.Front;
import forge.game.research.decision.infosupport.BallotBox;
import forge.game.research.decision.infosupport.PayForCosts;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static forge.game.ability.AbilityKey.Player;
import static java.lang.Double.NEGATIVE_INFINITY;

public class Facade {

    public Player controller;
    public ArrayList<SpellAbility> plays = new ArrayList<>();

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
     *
     * @return
     */
    public ArrayList<SpellAbility> getNextPlay(){
        if (plays.isEmpty() || !plays.get(0).canPlay())
            return null;
        else {
            ArrayList<SpellAbility> playNow = new ArrayList<>();
            playNow.add(plays.get(0));
            plays.remove(0);
            return playNow;
        }
    }

    public void getTurnPlays(){
        //Does this show up now?
        plays = new ArrayList<>();
        SpellAbility chosen;
        do {
            PlayCards pc = new PlayCards(controller);
            chosen = (pc.playLand(plays));
            if (chosen!= null) {
                plays.add(chosen);
            }
        } while(chosen!= null);

        printPlays();

    }

    private void printPlays(){
        System.out.println("     This turn, " + controller + " will play: ");
        for (SpellAbility sa: plays) {
            System.out.print(sa.getHostCard().getName() + " | ");
        }
        System.out.println();
    }

    public boolean payCosts(List<CostPart> costs, SpellAbility sp) {
        PayForCosts payer = new PayForCosts();
        return payer.payTheManaCost(controller, costs, sp, plays);
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

    /**
     * Evaluates the opponent's battlefield and determines what is the biggest threat through
     * highest value card
     * @return card with the biggest value on the opponent's side of the field
     */
     public Card isBiggestThreat () {
         Front f = new Front();

         //obtains the opponent
         Player opponent = this.controller.getSingleOpponent();
         Card threat = null;
         double cardvalue = NEGATIVE_INFINITY;

         //iterates through opponent's battlefield
         for (Card card : opponent.getCardsIn(ZoneType.Battlefield)) {
            if (!(card.isLand())) {
                //if there is a card that is valued higher, assigns that card to be returned
                if (f.chooser(card) > cardvalue) {
                    cardvalue = f.chooser(card);
                    threat = card;
                }
             }
         }

         return threat;
     }

}
