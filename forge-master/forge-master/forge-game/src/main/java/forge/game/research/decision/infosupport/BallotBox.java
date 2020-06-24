/**
 * Returns which card to play using a 'voting' system
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 22 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.player.Player;
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;

import static forge.game.research.decision.strategy.DeckStrategies.*;

public class BallotBox {
    public Player controller;
    public ArrayList<SpellAbility> nonlands;
    public ArrayList<Card> lands;


    public boolean needWhite;
    public boolean needBlue;
    public boolean needBlack;
    public boolean needGreen;
    public boolean needRed;
    public boolean canPlayTappedLand;

    public static final String RED = "R";
    public static final String GREEN = "G";
    public static final String BLUE = "U";
    public static final String BLACK = "B";
    public static final String WHITE = "W";

    /**
     * Takes in the player, assigns it to the controller variable
     * @param p
     */
    public BallotBox(Player p){
        controller = p;
    }

    public BallotBox(){}

    /**
     * TODO: description
     */
    public void getOptions(boolean potential) {
        ViablePlays vp = new ViablePlays(controller);
        if (potential) {
            nonlands = vp.getPotentialPlays();
        } else {
            nonlands = vp.getNonlandPlays();
        }
        lands = vp.getLandPlays();
    }

    /**
     * TODO: description
     * @param deckstrategy
     * @return TODO
     */
    public DoublyLinkedList<StrategyNode> getVotes(DeckStrategy deckstrategy){
        DoublyLinkedList<StrategyNode> votednodes = new DoublyLinkedList<StrategyNode>();
        for(Strategy strategy : deckstrategy.getStrategies()){
            //TODO: make this a general case/put in a specific passed in strategy
            votednodes.push_front(getViableNode(strategy));
        }
        return votednodes;
    }

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return voted node
     */
    public SpellAbility votedCard(DeckStrategy deckStrategy, boolean potential) {
        getOptions(potential);
        HashMap<SpellAbility, Integer> votesofcards = new HashMap<SpellAbility, Integer>();
        for (SpellAbility option : nonlands) {
            votesofcards.put(option, 0);
        }

        for (Strategy strat : deckStrategy.getStrategies()) {
            StrategyNode node = getViableNode(strat);
            //Find card from node
            if (node != null && node.getCards() != null) {
                for (CardTemplate template : node.getCards()) {
                    for (SpellAbility option : nonlands) {
                        if (template.matches(option)) {
                            int tempvotes = votesofcards.get(option);
                            votesofcards.replace(option, tempvotes+1);
                        }
                    }
                }
            }

        }

        int max = 0;
        SpellAbility chosen = null;
        for(SpellAbility option : votesofcards.keySet()){
            if(votesofcards.get(option) > max){
                max = votesofcards.get(option);
                if (option != null) {
                    chosen = option;
                }
            }
        }

        return chosen;
    }


    //TODO: Please break this into pieces. Please... Please.
    public Card choseLand(DeckStrategy deckStrategy) {
        //Checks to see if we can play a land at all
        if (!(controller.getLandsPlayedThisTurn() < controller.getMaxLandPlays())
                || !controller.canCastSorcery()
                || controller.getZone(ZoneType.Hand).getNumLandsIn() == 0) {
            return null;
        }

        //Initialize variables
        needWhite = false;
        needBlue = false;
        needBlack = false;
        needGreen = false;
        needRed = false;
        canPlayTappedLand = false;


        //More varaible initialization
        SpellAbility chosen = votedCard(deckStrategy, true);

        ManaEvaluation manaEval = new ManaEvaluation(controller);
        ArrayList<Integer> pool = manaEval.getManaCurrent();


        if (chosen != null) {
            int[] colors = chosen.getPayCosts().getTotalMana().getColorShardCounts();

            //Checks to see which color we need to play a card. It should only be one.
            if (pool.get(1) < colors[0]) {
                needWhite = true;
            } else if (pool.get(2) < colors[1]) {
                needBlue = true;
            } else if (pool.get(3) < colors[2]) {
                needBlack = true;
            } else if (pool.get(4) < colors[3]) {
                needRed = true;
            } else if (pool.get(5) < colors[4]) {
                needGreen = true;
            } else if (pool.get(0) >= chosen.getPayCosts().getTotalMana().getCMC()) {
                canPlayTappedLand = true;
            }
        }

        //Now that we know what kind of land we need, we must look for that land.
        for (Card land: lands) {
            if (canPlayTappedLand &&
                            land.entersTapped()) {
                return land;
            } else  if ((needBlack ||
                        needBlue ||
                        needGreen ||
                        needRed ||
                        needWhite) &&
                        !land.entersTapped()){
                for (SpellAbility sa: land.getManaAbilities()) {
                    String type = sa.getMapParams().get("Produced");
                    if (type.contains(RED) && needRed) {
                        return land;
                    }
                    if (type.contains(BLUE) && needBlue) {
                        return land;
                    }
                    if (type.contains(GREEN) && needGreen) {
                        return land;
                    }
                    if (type.contains(WHITE) && needWhite) {
                        return land;
                    }
                    if (type.contains(BLACK) && needBlack) {
                        return land;
                    }
                }
            } else {
                for (SpellAbility sa : land.getManaAbilities()) {
                    String type = sa.getMapParams().get("Produced");
                    if (type.contains(WHITE) && pool.get(1).equals(0)) {
                        return land;
                    }
                    if (type.contains(BLUE) && pool.get(2).equals(0)) {
                        return land;
                    }
                    if (type.contains(BLACK) && pool.get(3).equals(0)) {
                        return land;
                    }
                    if (type.contains(RED) && pool.get(4).equals(0)) {
                        return land;
                    }
                    if (type.contains(GREEN) && pool.get(5).equals(0)) {
                        return land;
                    }
                }
            }
        }
        //If we don't need or can utilize a specific land, just play the first one.
        return lands.get(0);

    }

    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public StrategyNode getViableNode(Strategy strategy){
        StrategyNode current = new StrategyNode();
        strategy.reset();
        while (current != null && !current.isViable(nonlands, controller)){
            if (strategy.hasNext()) {
                current = strategy.next();
            } else {
                strategy.next();
                return null;
            }
        }
        return current;

    }
}
