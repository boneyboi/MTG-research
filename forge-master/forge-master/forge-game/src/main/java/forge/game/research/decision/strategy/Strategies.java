/**
 * The strategies for decks (esp. our black/white lifelink, monored calvacade, and black/blue surveil)
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy;


import forge.game.research.decision.strategy.template.TemplateCMC;
import forge.game.research.decision.strategy.template.TemplateName;

import java.util.ArrayList;

public class Strategies {
    public ArrayList<Strategy> lifelinkstrats;
    public ArrayList<Strategy> monoredStrats;

    public Strategies(){
        lifelinkstrats = new ArrayList<Strategy>();
        lifelinkstrats.add(new Strategy("LifeLink"));
        lifelinkstrats.get(0).pushCard(new TemplateName("Daxos"));


        monoredStrats = new ArrayList<Strategy>();
        monoredStrats.add(new Strategy("Monored"));
        monoredStrats.get(0).pushCard(new TemplateCMC(3));
        monoredStrats.get(0).pushCard(new TemplateCMC(2));
        monoredStrats.get(0).pushCard(new TemplateCMC(1));
    }

}
