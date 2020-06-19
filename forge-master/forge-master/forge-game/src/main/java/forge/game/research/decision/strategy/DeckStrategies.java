/**
 * The strategies for decks (esp. our black/white lifelink, monored calvacade, and black/blue surveil)
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy;


import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.research.decision.strategy.template.TemplateCMC;
import forge.game.research.decision.strategy.template.TemplateName;

import java.util.ArrayList;

public class DeckStrategies {
    public ArrayList<Strategy> lifelinkstrats;
    public ArrayList<Strategy> monoredStrats;

    public DeckStrategies(){
        lifelinkstrats = new ArrayList<Strategy>();
        addStrategy(this.lifelinkstrats, "LifeLink");
        addTemplateCard(this.lifelinkstrats, 0, new TemplateName("Daxos"));


        monoredStrats = new ArrayList<Strategy>();
        addStrategy(this.monoredStrats, "Monored");
        addTemplateCard(this.monoredStrats, 0, new TemplateCMC(1));
        addTemplateCard(this.monoredStrats, 0, new TemplateCMC(2));
        addTemplateCard(this.monoredStrats, 0, new TemplateCMC(3));
    }

    public void addStrategy(ArrayList<Strategy> strategy, String name){
        strategy.add(new Strategy(name));
    }
    public void addTemplateCard(ArrayList<Strategy> strategy, int index, CardTemplate template){
        strategy.get(index).pushCard(template);
    }
    public void addTemplateRequirement(ArrayList<Strategy> strategy, int index, CardTemplate template){
        strategy.get(index).pushReq(template);
    }
}
