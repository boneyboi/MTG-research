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
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class DeckStrategy {

    private ArrayList<Strategy> deckStrategy;
    private HashMap<String, Object> parameters = new HashMap<String, Object>();

    public DeckStrategy(String name){
        this.deckStrategy = new ArrayList<Strategy>();
        this.addParameter("name", name);
    }

    public void addStrategy(String name){ this.deckStrategy.add(new Strategy(name)); }

    public void addNode(StrategyNode node) {
        this.getStrategies().get(this.getStrategies().size()-1).pushFront(new StrategyNode(node));
    }

    public void addNode(String name, StrategyNode node) {
        for (Strategy strat: deckStrategy) {
            if (strat.getName().equals(name)) {
                strat.pushFront(new StrategyNode(node));
                return;
            }
        }
        System.out.println("No strategy of name " + name + " found.");
    }

    public void addNode(int index, StrategyNode node){
        this.getStrategies().get(index).pushFront(new StrategyNode(node));
    }
    public void addTemplateCard(String name, CardTemplate template){
        for (Strategy strat: deckStrategy) {
            if (strat.getName().equals(name)) {
                strat.pushCard(0, template);
                return;
            }
        }
        System.out.println("No strategy of name " + name + " found.");
    }

    public void addTemplateRequirement(String name, CardTemplate template){
        for (Strategy strat: deckStrategy) {
            if (strat.getName().equals(name)) {
                strat.pushReq(0, template);
                return;
            }
        }
        System.out.println("No strategy of name " + name + " found.");
    }

    public void addTemplateCard(int index, CardTemplate template){
        this.deckStrategy.get(index).pushCard(0, template);
    }
    public void addTemplateRequirement(int index, CardTemplate template){
        this.deckStrategy.get(index).pushReq(0, template);
    }

    public void addTemplateCard(CardTemplate template){
        this.deckStrategy.get(deckStrategy.size()-1).pushCard(0, template);
    }

    public void addTemplateRequirement(CardTemplate template){
        this.deckStrategy.get(deckStrategy.size()-1).pushReq(0, template);
    }

    /**
     * Return the list of strategies
     * @return
     */
    public ArrayList<Strategy> getStrategies(){
        return this.deckStrategy;
    }

    public String getName(){
        return (String)this.getParameter("name");
    }

    public void addParameter(String key, Object value){
        parameters.put(key, value);
    }
    public void setParameter(String key, Object value){
        parameters.replace(key, value);
    }
    public Object getParameter(String key){
        if(parametersContains(key)){
            return parameters.get(key);
        } else {
            return "No such key found";
        }

    }
    public Set<String> getParameterKeys(){
        return parameters.keySet();
    }
    public Collection<Object> getParameterValues(){
        return parameters.values();
    }
    public boolean parametersContains(String key){
        return parameters.containsKey(key);
    }
}
