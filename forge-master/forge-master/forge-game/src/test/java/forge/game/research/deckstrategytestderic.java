package forge.game.research;

import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.research.decision.strategy.template.TemplateName;
import org.junit.Test;

public class deckstrategytestderic {

    public DeckStrategy createdeckstrategy(){
        int stratindex = 0;
        DeckStrategy deckstrategyresult = new DeckStrategy("bruh strategy");
        deckstrategyresult.addStrategy("bruh strategy1");
        System.out.println(deckstrategyresult.getStrategies().get(stratindex).getName());
        int cardindex = 0;
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard1"));
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard2"));
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard3"));
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard4"));

        deckstrategyresult.addStrategy("bruh strategy2");
        System.out.println(deckstrategyresult.getStrategies().get(++stratindex).getName());
        cardindex = 0;
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard21"));
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard22"));
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard23"));
        deckstrategyresult.getStrategies().get(stratindex).pushFront(new StrategyNode());
        deckstrategyresult.getStrategies().get(stratindex).pushCard(cardindex++, new TemplateName("bruhcard24"));

        deckstrategyresult.addStrategy("bruh strategy3");
        System.out.println(deckstrategyresult.getStrategies().get(++stratindex).getName());

        deckstrategyresult.addStrategy("bruh strategy4");
        System.out.println(deckstrategyresult.getStrategies().get(++stratindex).getName());
        return deckstrategyresult;
    }


    @Test
    public void testcreatedeckstrategy(){
        DeckStrategy deckstrategy = createdeckstrategy();

        for(Strategy s : deckstrategy.getStrategies()){
            s.reset();
            while(s.hasNext()){
                StrategyNode snode = s.next();
                DoublyLinkedList<CardTemplate> cards = snode.getCards();
                CardTemplate c = snode.nextCard();
                if(c.getClass().equals(TemplateName.class)){
                    System.out.println(((TemplateName)c).getName());
                }
            }
        }

    }
}
