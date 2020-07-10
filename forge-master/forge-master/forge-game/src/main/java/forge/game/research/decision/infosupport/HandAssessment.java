package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class HandAssessment {

    public Player controller;
    public DeckStrategy strategy;
    private static final double WIDTHREQ = .3;
    private static final double DEPTHREQ = .3;

    public HandAssessment(Player player, DeckStrategy strats) {
        controller = player;
        strategy = strats;
    }

    public CardCollectionView getHand() {
        return controller.getZone(ZoneType.Hand).getCards();
    }

    /**
     * Determines whether a hand is good enough to keep
     * @return
     */
    public boolean judgeHand() {
        ArrayList<Card> hand = new ArrayList<>();
        for (Card c: controller.getZone(ZoneType.Hand)) {
            hand.add(c);
        }
        ArrayList<Double> map = assessHand(hand);
        //TODO: Make these reqs variable.
        return map.get(0) > WIDTHREQ && map.get(1) > DEPTHREQ;
    }

    /**
     * Returns whether a hand is acceptable to keep or not
     * @return
     */
    public ArrayList<Double> assessHand(ArrayList<Card> hand) {
        double depth = 0;
        double width = 0;
        double total = 0;
        ArrayList<Double> toReturn = new ArrayList<>();
        for (Strategy path: strategy.getStrategies()) {
            int temp = NumNodesViable(path, hand);
            total += path.size();
            if (temp != 0) {
                width++;
                depth += temp;
                path.size();
            }

        }
        if (strategy.getStrategies().size() == 0) {
            toReturn.add(0.0);
        } else {
            toReturn.add(width / strategy.getStrategies().size());
        }

        if (total == 0) {
            toReturn.add(0.0);
        } else {
            toReturn.add(depth / total);
        }
        return toReturn;
    }

    public ArrayList<Card> compareHands(ArrayList<Card> hand1, ArrayList<Card> hand2) {
        ArrayList<Double> hand1val = assessHand(hand1);
        ArrayList<Double> hand2val = assessHand(hand2);
        if (hand1val.get(0) > hand2val.get(0) && hand1val.get(1) > hand2val.get(1)) {
            return hand1;
        } else if (hand1val.get(0) < hand2val.get(0) && hand1val.get(1) < hand2val.get(1)) {
            return hand2;
        }
        if (hand1val.get(0) + hand1val.get(1) > hand2val.get(0) + hand2val.get(1)) {
            return hand1;
        }
        return hand2;
    }

    public int NumNodesViable(Strategy strategy, ArrayList<Card> cards) {
        ViablePlays vp = new ViablePlays(controller);
        ArrayList<SpellAbility> options = vp.getPossibilities(cards);
        int count = 0;
        StrategyNode current;
        strategy.reset();
        while (strategy.hasNext()) {
            current = strategy.next();
            if (current != null && current.isViable(options, controller)) {
                count ++;
            }
        }

        return count;

    }

}
