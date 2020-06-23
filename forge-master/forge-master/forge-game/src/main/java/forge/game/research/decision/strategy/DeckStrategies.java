/**
 * The strategies for decks (esp. our black/white lifelink, monored calvacade, and black/blue surveil)
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.strategy;


import forge.game.research.decision.strategy.template.*;

import java.util.ArrayList;

public class DeckStrategies {
    public static DeckStrategy lifelinkstrats;
    public static DeckStrategy monoredStrats;

    public DeckStrategies(){
        deckStratInit();
    }

    public void deckStratInit(){
        this.lifelinkstrats = new DeckStrategy();
        this.lifelinkstrats.addStrategy("Curve");
        this.lifelinkstrats.addTemplateCard(0, new TemplatePermanentCMC(1));
        this.lifelinkstrats.addTemplateCard(0, new TemplatePermanentCMC(2));
        this.lifelinkstrats.addTemplateCard(0, new TemplatePermanentCMC(3));
        this.lifelinkstrats.addTemplateCard(0, new TemplatePermanentCMC(4));
        this.lifelinkstrats.addTemplateCard(0, new TemplatePermanentCMC(5));

        this.lifelinkstrats.addStrategy("Daxos");
        this.lifelinkstrats.addTemplateCard(1, new TemplateName("Daxos, Blessed by the Sun"));

        this.lifelinkstrats.addStrategy("Life Combo 1");
        this.lifelinkstrats.addTemplateCard(2, new TemplateLifelink());
        this.lifelinkstrats.addTemplateCard(2, new TemplateLifeBuff());

        this.lifelinkstrats.addStrategy("Life Combo 2");
        this.lifelinkstrats.addTemplateCard(3, new TemplateLifelink());
        this.lifelinkstrats.addTemplateCard(3, new TemplateName("Daxos, Blessed by the Sun"));
        this.lifelinkstrats.addTemplateCard(3, new TemplateLifeBuff());
        this.lifelinkstrats.addTemplateCard(3, new TemplateLifelink());
        this.lifelinkstrats.addTemplateCard(3, new TemplateName("Twinblade Paladin"));

        this.monoredStrats = new DeckStrategy();
        this.monoredStrats.addStrategy("Monored");
        this.monoredStrats.addTemplateCard(0, new TemplatePermanentCMC(1));
        this.monoredStrats.addTemplateCard(0, new TemplatePermanentCMC(2));
        this.monoredStrats.addTemplateCard(0, new TemplatePermanentCMC(3));
    }

}
