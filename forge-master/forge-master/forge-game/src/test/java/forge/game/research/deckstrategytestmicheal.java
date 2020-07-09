package forge.game.research;

import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.TemplatePermanentCMC;
import org.junit.Test;

public class deckstrategytestmicheal {

    @Test
    public void testBuild() {
        DeckStrategy testing = new DeckStrategy("test strat");
        assert  (testing.getStrategies().isEmpty()) : "testing not created";
    }

    @Test
    public void testAddStrategy() {
        DeckStrategy testing = new DeckStrategy("test strat");
        testing.addStrategy("Curve");
        testing.addStrategy("Test");
        assert (testing.getStrategies().size() == 2) : "There are not enough strategies";
        assert (testing.getStrategies().get(0).size() == 0) : "Strategies are created with a node.";
        assert (testing.getStrategies().get(0).getName().equals("Curve"));

    }

    @Test
    public void testAddNodes() {
        DeckStrategy testing = new DeckStrategy("test strat");
        testing.addStrategy("Curve");
        StrategyNode node = new StrategyNode(false);
        node.addCard(new TemplatePermanentCMC(1));
        testing.addNode(0, node);
    }
}
