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
import forge.game.spellability.LandAbility;
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

    public static final double STRATSIZEMULTIPLIER = 5;
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
    public void getOptions(boolean potential, ArrayList<SpellAbility> plays) {
        ViablePlays vp = new ViablePlays(controller);
        if (potential) {
            nonlands = vp.getPotentialPlays();
        } else {
            nonlands = vp.getNonlandPlaysAfter(plays);
        }
    }


    public void printStrategy(DeckStrategy deckStrategy) {
        for (Strategy strat: deckStrategy.getStrategies()) {
            System.out.print("Strategy: " + strat.getName());
            System.out.println();
            StrategyNode current;
            strat.reset();
            while (strat.hasNext()) {
                current = strat.next();
                if (current.getCards().isEmpty()) {
                    System.out.print('E');
                } else if (current.isDone(controller)) {
                    System.out.print('O');
                } else if(current.isPossible(controller)){
                    System.out.print('P');
                } else {
                    System.out.print('X');
                }
                System.out.print('-');
            }
            System.out.println('|');
        }
    }

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return voted node
     */
    public SpellAbility votedCard(DeckStrategy deckStrategy,
                                  boolean potential, ArrayList<SpellAbility> plays) {
        getOptions(potential, plays);
        HashMap<SpellAbility, Integer> votesofcards = new HashMap<>();
        if (nonlands.isEmpty()) {
            return null;
        }
        for (SpellAbility option : nonlands) {
            votesofcards.put(option, 0);
        }

        for (Strategy strat : deckStrategy.getStrategies()) {
            //Account for depth of strategy, progression of strategy, and
            //possibly index of strategy (prioritization)
            ArrayList<StrategyNode> viables = getViableNode(strat);

            //depth is (#done, #possible, #total)
            ArrayList<Integer> depth = getDepth(strat);
            double completeable = depth.get(0) + depth.get(1);
            double total = depth.get(2);
            double ratio = completeable/total;
            double stratWeight = ratio*100+ STRATSIZEMULTIPLIER*total;
            double count = 0;

            //Find card from node
            for (StrategyNode node: viables) {
                if (node != null && node.getCards() != null) {
                    double nodeWeight = (1.0 - count/viables.size());
                    int voteWeight = (int) (stratWeight * nodeWeight);
                    for (CardTemplate template : node.getCards()) {
                        for (SpellAbility option : nonlands) {
                            if (template.matches(option)) {
                                int tempvotes = votesofcards.get(option);
                                votesofcards.replace(option, tempvotes + voteWeight);
                            }
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


    public void printVotes(SpellAbility sa, int votes) {
            System.out.println(sa + " got " + votes + " votes.");
    }

    //TODO: Please break this into pieces. Please... Please.
    public Card choseLand(DeckStrategy deckStrategy, ArrayList<SpellAbility> playing) {
        //Checks to see if we can play a land at all
        if (!(controller.getLandsPlayedThisTurn() < controller.getMaxLandPlays())
                || controller.getZone(ZoneType.Hand).getNumLandsIn() == 0) {
            return null;
        }

        //Get avaliable lands
        ViablePlays vp = new ViablePlays(controller);
        lands = vp.getLandPlays();

        //See if we are already playing a land
        //TODO: this needs a better solution, since sometimes we can
        //play multiple lands a turn.
        for (SpellAbility sa : playing) {
            if (sa instanceof LandAbility) {
                return null;
            }
        }

        //Initialize variables
        needWhite = false;
        needBlue = false;
        needBlack = false;
        needGreen = false;
        needRed = false;
        canPlayTappedLand = false;


        //Find the first spell we want to play
        //TODO: Adjust this for multiple cards to play.
        SpellAbility chosen = votedCard(deckStrategy, true, new ArrayList<>());

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
                //If we don't need a color, we can play a tap land
                canPlayTappedLand = true;
            }
        } else {
            canPlayTappedLand = true;
        }

        //Now that we know what kind of land we need, we must look for that land.

        //Can we play a tap land?
        for (Card land : lands) {
            if (canPlayTappedLand &&
                    land.entersTapped()) {
                return land;
            }
        }
        //Do we need a certain color?
        for (Card land : lands) {
            if ((needBlack ||
                    needBlue ||
                    needGreen ||
                    needRed ||
                    needWhite) &&
                    !land.entersTapped()) {
                for (SpellAbility sa : land.getManaAbilities()) {
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
            }
        }
        //Can we get more colors into our pool?
        for (Card land : lands) {
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
        //If we don't need or can utilize a specific land, just play the first one.
        return lands.get(0);
    }

    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public ArrayList<StrategyNode> getViableNode(Strategy strategy){
        ArrayList<StrategyNode> viables = new ArrayList<>();
        StrategyNode current = new StrategyNode();
        strategy.reset();
        while (current != null){
            if (current.isViable(nonlands, controller)){
                viables.add(new StrategyNode(current));
            }
            if (strategy.hasNext()) {
                current = strategy.next();
            } else {
                current = null;
            }
        }
        return viables;

    }

    /**
     * Determines the length of a strategy, how much we have already done,
     * and how many nodes we can do this turn. Returns what we have done, what we can do,
     * and how many nodes there are in that order.
     */
    public ArrayList<Integer> getDepth(Strategy strat) {
        StrategyNode current;
        int done = 0;
        int possible = 0;
        ArrayList<Integer> returns = new ArrayList<>();
        strat.reset();
        if (strat.hasNext()) {
            do {
                current = strat.next();
                if (current != null && current.isDone(controller)) {
                    done++;
                }
                if (current != null && current.isPossible(controller)) {
                    possible++;
                }
            } while (strat.hasNext());
        }
        returns.add(done);
        returns.add(possible);
        returns.add(strat.size());
        return returns;
    }
}
