/**
 * The strategies for decks (esp. our black/white lifelink, monored calvacade, and black/blue surveil)
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 23 June 2020
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
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplatePermanentCMC(1));
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplatePermanentCMC(2));
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplatePermanentCMC(3));
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplatePermanentCMC(4));
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplatePermanentCMC(5));


        this.lifelinkstrats.addStrategy("Daxos");
        this.lifelinkstrats.addNode(new StrategyNode(false));
        this.lifelinkstrats.addTemplateCard(new TemplateName("Daxos, Blessed by the Sun"));

        this.lifelinkstrats.addStrategy("Life Combo 1");
        //TODO: This is put in one Node?
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplateLifeBuff());
        this.lifelinkstrats.addNode(new StrategyNode(false));
        this.lifelinkstrats.addTemplateCard(new TemplateLifelink());

        this.lifelinkstrats.addStrategy("Removal");
        //TODO: Repeat set to false by default?
        this.lifelinkstrats.addNode(new StrategyNode());
        this.lifelinkstrats.addTemplateCard(new TemplateRemoval());


        this.monoredStrats = new DeckStrategy();
        this.monoredStrats.addStrategy("Monored");
        this.monoredStrats.addNode(new StrategyNode());
        this.monoredStrats.addTemplateCard(new TemplatePermanentCMC(1));
        this.monoredStrats.addNode(new StrategyNode());
        this.monoredStrats.addTemplateCard(new TemplatePermanentCMC(2));
        this.monoredStrats.addNode(new StrategyNode());
        this.monoredStrats.addTemplateCard(new TemplatePermanentCMC(3));

        this.monoredStrats.addStrategy("ChandraCade");
        this.monoredStrats.addNode(new StrategyNode());
        this.monoredStrats.addTemplateCard(new TemplateName( "Charndra's Spitfire"));
        this.monoredStrats.addNode(new StrategyNode());
        this.monoredStrats.addTemplateCard(new TemplateName("Cavalcade of Calamity"));
        this.monoredStrats.addNode(new StrategyNode());
        this.monoredStrats.addTemplateRequirement(new TemplateName("Chandra's Spitfire"));


    }

}
