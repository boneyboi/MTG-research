/**
 * Class that returns the decision when the computer asks for one
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since July 07, 2020
 */
package forge.game.research.decision;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.cost.CostPart;
import forge.game.player.Player;
import forge.game.research.PlayCards;
import forge.game.research.decision.infosupport.BallotBox;
import forge.game.research.decision.infosupport.PayForCosts;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.spellability.SpellAbility;

public class Facade {

    public Player controller;
    public ArrayList<SpellAbility> plays = new ArrayList<>();
    public DeckStrategies trial;
    private DeckStrategy plan;

    public Facade(Player p) {
        controller = p;
        trial = new DeckStrategies();
        plan = getDeckStrat(trial.getDecks());
        System.out.println(plan.getName());
    }

    /**
     * Use this constructor only for testing reasons really.
     */

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
            PlayCards pc = new PlayCards(controller, plan);
            chosen = (pc.playLand(plays));
            if (chosen!= null) {
                plays.add(chosen);
            }
        } while(chosen!= null);

        printStrategies(plan);

    }

    public void printStrategies(DeckStrategy deckstrat){
        BallotBox box = new BallotBox(controller);
        box.printStrategy(deckstrat);
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
     * Method gets the name of a deck.
     * @return String, name of Ai's deck
     */
     private String getNameDeckAi () {

         String deckName = this.controller.getRegisteredPlayer().getDeck().getName();

         return deckName;
     }

     //TODO: Finish this when we can obtain a list of deck strategy
    /**
     * Method gets the name of a strategy based on name of a deck
     * @return Strategy
     */
    public DeckStrategy getDeckStrat(ArrayList<DeckStrategy> deckStrategyList) {
        DeckStrategy ds = null;
        String deckName = getNameDeckAi();
        ArrayList<DeckStrategy> allDS = deckStrategyList;

        for (DeckStrategy strat : allDS) {
            if (deckName.equalsIgnoreCase(strat.getName())) {
                ds = strat;
            }
        }

        if (ds == null) {
            ds = allDS.get(0);
        }

        return ds;
    }

}
