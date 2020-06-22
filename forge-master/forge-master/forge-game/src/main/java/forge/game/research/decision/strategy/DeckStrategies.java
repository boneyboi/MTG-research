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
    public static DeckStrategy lifelinkstrats;
    public static DeckStrategy monoredStrats;

    public DeckStrategies(){
        lifelinkstrats = new DeckStrategy();
        lifelinkstrats.addStrategy("LifeLink");
        lifelinkstrats.addTemplateCard(0, new TemplateName("Daxos"));


        monoredStrats = new DeckStrategy();
        monoredStrats.addStrategy("Monored");
        monoredStrats.addTemplateCard(0, new TemplateCMC(1));
        monoredStrats.addTemplateCard(0, new TemplateCMC(2));
        monoredStrats.addTemplateCard(0, new TemplateCMC(3));
    }


}
