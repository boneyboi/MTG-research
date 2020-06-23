/**
 * TODO: description
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 23 June 2020
 */

package forge.game.research.decision.strategy;

import forge.game.research.decision.strategy.template.CardTemplate;

import java.util.ArrayList;

public class DeckStrategy {

    private ArrayList<Strategy> deckStrategy;

    public DeckStrategy(){
        this.deckStrategy = new ArrayList<Strategy>();
    }

    public void addStrategy(String name){ this.deckStrategy.add(new Strategy(name)); }
    public void addNode(int index, StrategyNode node){
            this.getStrategies().get(index).pushFront(this.getStrategies().get(index), new StrategyNode(node));
    }
    public void addTemplateCard(int index, CardTemplate template){
        this.deckStrategy.get(index).pushCard(template);
    }
    public void addTemplateRequirement(int index, CardTemplate template){
        this.deckStrategy.get(index).pushReq(template);
    }
    public ArrayList<Strategy> getStrategies(){
        return this.deckStrategy;
    }
}
