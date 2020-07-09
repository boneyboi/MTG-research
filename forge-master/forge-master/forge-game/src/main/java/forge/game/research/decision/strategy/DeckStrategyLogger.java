package forge.game.research.decision.strategy;

import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.research.decision.strategy.template.TemplateName;
import forge.game.research.decision.strategy.template.TemplateNonPermanentCMC;
import forge.game.research.decision.strategy.template.TemplatePermanentCMC;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeckStrategyLogger {

    private final static Logger deckStrategyLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private FileHandler fh;
    public DeckStrategyLogger(String filePath) throws IOException {
        File f = new File(filePath);
        fh = new FileHandler(f.getCanonicalPath());
        deckStrategyLogger.addHandler(fh);
    }

    public void logDeckStrategy(DeckStrategy deckStrategy){
        deckStrategyLogger.log(Level.INFO, "Deck Strategy: " + deckStrategy.getName());
        for(Strategy s : deckStrategy.getStrategies()){
            deckStrategyLogger.log(Level.INFO, "Strategy: " +s.getName() + "\n" +
                                                    "Cards in this strategy:\n");
            s.reset();
            while(s.hasNext()){
                StrategyNode snode = s.next();
                DoublyLinkedList<CardTemplate> cards = snode.getCards();
                CardTemplate c = snode.nextCard();
                if(c.getClass().equals(TemplateName.class)){
                    System.out.println(((TemplateName)c).getName());
                    deckStrategyLogger.log(Level.INFO, ((TemplateName)c).getName());
                }
                else if(c.getClass().equals(TemplatePermanentCMC.class)){
                    System.out.println(((TemplatePermanentCMC)c).getCMC());
                    deckStrategyLogger.log(Level.INFO, ""+(((TemplatePermanentCMC)c).getCMC()));
                }
                else if(c.getClass().equals(TemplateNonPermanentCMC.class)){
                    System.out.println(((TemplateNonPermanentCMC)c).getCMC());
                    deckStrategyLogger.log(Level.INFO, ""+(((TemplateNonPermanentCMC)c).getCMC()));
                } else {
                    System.out.println(c.toString());
                    deckStrategyLogger.log(Level.INFO, c.toString());
                }
            }
        }
    }

}
