/**
 * The strategies for decks (esp. our black/white lifelink, monored calvacade, and black/blue surveil)
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy;


import forge.game.research.decision.strategy.template.TemplatePermanentCMC;
import forge.game.research.decision.strategy.template.TemplateName;

public class DeckStrategies {
    public static DeckStrategy lifelinkstrats;
    public static DeckStrategy monoredStrats;

    public DeckStrategies(){
        lifelinkstrats = new DeckStrategy();
        lifelinkstrats.addStrategy("LifeLink");
        lifelinkstrats.addTemplateCard(0, new TemplateName("Daxos"));


        monoredStrats = new DeckStrategy();
        monoredStrats.addStrategy("Monored");
        monoredStrats.addTemplateCard(0, new TemplatePermanentCMC(1));
        monoredStrats.addTemplateCard(0, new TemplatePermanentCMC(2));
        monoredStrats.addTemplateCard(0, new TemplatePermanentCMC(3));
    }


}
