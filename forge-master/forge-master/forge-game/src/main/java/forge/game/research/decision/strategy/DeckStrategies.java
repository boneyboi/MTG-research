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
        this.lifelinkstrats = new DeckStrategy();
        this.lifelinkstrats.addStrategy("LifeLink");
        this.lifelinkstrats.addTemplateCard(0, new TemplateName("Daxos"));


        this.monoredStrats = new DeckStrategy();
        this.monoredStrats.addStrategy("Monored");
        this.monoredStrats.addTemplateCard(0, new TemplateCMC(1));
        this.monoredStrats.addTemplateCard(0, new TemplateCMC(2));
        this.monoredStrats.addTemplateCard(0, new TemplateCMC(3));
    }


}
