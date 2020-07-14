package forge.game.research;

import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.TemplatePermanentCMC;
import org.junit.Test;

import java.util.ArrayList;

public class deckstrategytestmicheal {

    @Test
    public void testBuild() {
        DeckStrategy testing = new DeckStrategy("test strat");
        assert  (testing.getStrategies().isEmpty()) : "testing not created";
    }

    public class Case {
        int option = 0;
        String name;
        public Case(String call) {
            name = call;
        }

        public Case(Case c) {
            this.name = c.name;
            this.option = c.option;
        }

        public int getOption() {
            return option;
        }

        public String getName() {
            return name;
        }

        public void setOption(int amt) {
            option = amt;
        }
    }

    @Test
    public void clock() {
        ArrayList<String> attacks = new ArrayList<>();
        attacks.add("a");
        attacks.add("b");
        attacks.add("c");
        attacks.add("d");
        attacks.add("e");
        attacks.add("f");
        attacks.add("g");
        attacks.add("h");
        attacks.add("i");
        attacks.add("j");
        attacks.add("end");

        ArrayList<Case> blocks = new ArrayList<>();
        blocks.add(new Case("1"));
        blocks.add(new Case("2"));
        blocks.add(new Case("3"));
        blocks.add(new Case("4"));
        blocks.add(new Case("5"));

        int loops = 0;
        boolean check = false;

        Case selected;
        boolean done;
        while (blocks.get(blocks.size()-1).getOption() != (attacks.size() - 1)) {
            if (blocks.get(0).getOption() == 7 &&
                blocks.get(1).getOption() == 7 &&
                blocks.get(2).getOption() == 7 &&
                blocks.get(3).getOption() == 7 &&
                blocks.get(4).getOption() == 7) {
                check = true;
            }
            loops ++;
            int get = 0;
            done = false;

            while (!done) {
                selected = blocks.get(get);
                selected.setOption(selected.getOption() + 1);
                if (selected.getOption() == (attacks.size() - 1)) {
                    if (selected != blocks.get(blocks.size() - 1)) {
                        selected.setOption(0);
                        get += 1;
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        }
        assert(check);
        assert (loops == 100000);
    }

    public void move() {

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
