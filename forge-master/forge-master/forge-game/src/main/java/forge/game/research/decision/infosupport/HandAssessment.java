package forge.game.research.decision.infosupport;

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

    public HandAssessment(Player player, DeckStrategy strats) {
        controller = player;
        strategy = strats;
    }

    public CardCollectionView getHand() {
        return controller.getZone(ZoneType.Hand).getCards();
    }

    /**
     * Returns whether a hand is acceptable to keep or not
     * @return
     */
    public boolean AssessHand() {
        int depth = 0;
        int width = 0;
        for (Strategy path: strategy.getStrategies()) {
            int temp = NumNodesViable(path);
            if (temp != 0) {
                width++;
                depth += temp;
            }

        }
        //TODO:
        return false;
    }

    public int NumNodesViable(Strategy strategy) {
        ViablePlays vp = new ViablePlays(controller);
        ArrayList<SpellAbility> options = vp.getPossibilities();
        int count = 0;
        StrategyNode current = new StrategyNode();
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
