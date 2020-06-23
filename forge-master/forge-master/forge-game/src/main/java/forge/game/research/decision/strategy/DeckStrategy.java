package forge.game.research.decision.strategy;

import forge.game.research.decision.strategy.template.CardTemplate;

import java.util.ArrayList;

public class DeckStrategy {

    private ArrayList<Strategy> deckStrategy;

    public DeckStrategy(){
        this.deckStrategy = new ArrayList<Strategy>();
    }

    public void addStrategy(String name){ this.deckStrategy.add(new Strategy(name)); }
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
