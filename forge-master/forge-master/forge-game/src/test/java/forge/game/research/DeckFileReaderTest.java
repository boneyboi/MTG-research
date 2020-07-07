package forge.game.research;

import com.google.gson.Gson;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.research.decision.strategy.*;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.research.decision.strategy.template.TemplateName;
import forge.game.research.decision.strategy.template.TemplatePermanentCMC;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class DeckFileReaderTest {
    @Test
    public void ClassConversionFromJsonTest(){
        Gson gson = new Gson();
        Strategy s = gson.fromJson(
                "{" +
                "'lifelink': {" +
                        "strategy : {'templatename' : 'daxos'}" +
                        "}" +
                "}", Strategy.class);
        System.out.println(s.getName());
    }

    @Test
    public void DeckConversionFromJsonTest() throws IOException {

        JsonDeckStrategy jsondeck = new JsonDeckStrategy();
        DeckStrategy deckstrategy = jsondeck.createDeckStrategy("C:\\Users\\deric\\Desktop\\MTG-research\\forge-master\\forge-master\\forge-game\\src\\main\\java\\forge\\game\\research\\decision\\Decks\\LifeLink.json");
        System.out.println(deckstrategy.getStrategies().get(0).getName());
        //Strategy temp = deckstrategy.getStrategies().get(0).;
        for(Strategy s : deckstrategy.getStrategies()){
            s.reset();
            for(CardTemplate c : s.get(0).getCards()){
                System.out.println(((TemplatePermanentCMC) c).getCMC());
            }
        }
        /*or(CardTemplate c : deckstrategy.getStrategies().get(0).next().getCards()){
            if(c.getClass().equals(TemplatePermanentCMC.class)){
                System.out.println(((TemplatePermanentCMC) c).getCMC());
            }
            System.out.println(c.toString());
        }*/
        //System.out.println(deckstrategy.getStrategies().get(0).next().getCards().peek_front().toString());

    }

    @Test
    public void hashmaptesting(){
        addMethod("bruh", bruh -> {String s = "bruh moment"; System.out.println(s); return null;});
        useMethod("bruh", "nothing lol");
    }
    public HashMap<String,Function> functions = new HashMap<>();
    public void addMethod(String name, Function func){
        functions.put(name, func);
    }
    public void useMethod(String name, String input){
        functions.get(name);
    }


    @Test
    public void addinggettingcardtest(){
        DeckStrategy deckStrategy = new DeckStrategy();
        deckStrategy.addStrategy("bruh");
        System.out.println(deckStrategy.getStrategies().get(0).getName());
        deckStrategy.getStrategies().get(0).pushFront(new StrategyNode());
        deckStrategy.getStrategies().get(0).reset();
        deckStrategy.getStrategies().get(0).pushCard(0, new TemplateName("bruhmoment"));
        deckStrategy.getStrategies().get(0).reset();
        System.out.println(((TemplateName)deckStrategy.getStrategies().get(0).next().nextCard()).getName());
    }
}
