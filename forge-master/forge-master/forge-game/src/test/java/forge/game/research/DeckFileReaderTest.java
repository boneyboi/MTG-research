package forge.game.research;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.research.decision.strategy.*;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.research.decision.strategy.template.TemplateName;
import forge.game.research.decision.strategy.template.TemplateNonPermanentCMC;
import forge.game.research.decision.strategy.template.TemplatePermanentCMC;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        DeckStrategy deckstrategy = jsondeck.createDeckStrategy("src\\main\\java\\forge\\game\\research\\decision\\Decks\\MonoRed.json");
        System.out.println(deckstrategy.getStrategies().get(0).getName());
        //Strategy temp = deckstrategy.getStrategies().get(0).;
        for(Strategy s : deckstrategy.getStrategies()){
            s.reset();
            while(s.hasNext()){
                StrategyNode snode = s.next();
                DoublyLinkedList<CardTemplate> cards = snode.getCards();
                CardTemplate c = snode.nextCard();
                System.out.println(c.toString());
                if(c.getClass().equals(TemplateName.class)){
                    System.out.println(((TemplateName)c).getName());
                }
                else if(c.getClass().equals(TemplatePermanentCMC.class)){
                    System.out.println(((TemplatePermanentCMC)c).getCMC());
                }
                else if(c.getClass().equals(TemplateNonPermanentCMC.class)){
                    System.out.println(((TemplateNonPermanentCMC)c).getCMC());
                }
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
        DeckStrategy deckStrategy = new DeckStrategy("bruh strategy");
        deckStrategy.addStrategy("bruh");
        System.out.println(deckStrategy.getStrategies().get(0).getName());
        deckStrategy.getStrategies().get(0).pushFront(new StrategyNode());
        deckStrategy.getStrategies().get(0).reset();
        deckStrategy.getStrategies().get(0).pushCard(0, new TemplateName("bruhmoment"));
        deckStrategy.getStrategies().get(0).reset();
        System.out.println(((TemplateName)deckStrategy.getStrategies().get(0).next().nextCard()).getName());
    }

    @Test
    public void DeckLoggerTest() throws IOException {
        JsonDeckStrategy jsondeck = new JsonDeckStrategy();
        DeckStrategy deckstrategy = jsondeck.createDeckStrategy("src\\main\\java\\forge\\game\\research\\decision\\Decks\\MonoRed.json");
        DeckStrategyLogger dsl = new DeckStrategyLogger("src\\test\\java\\forge\\game\\research\\test\\test.log");
        dsl.logDeckStrategy(deckstrategy);
    }

    @Test
    public void FullTest() {
        ArrayList<DeckStrategy> decks = new ArrayList<>();
        JsonDeckStrategy builder = new JsonDeckStrategy();
        File folder = new File("..\\..\\forge-master\\forge-game\\src\\main\\java\\forge\\game\\research\\decision\\Decks");
        Path dir = folder.toPath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)){
            for (Path file: stream) {
                DeckStrategy temp = builder.createDeckStrategy(file.toString());
                decks.add(temp);
                assert(true) : "We added decks";
            }
        } catch (IOException e) {
            e.printStackTrace();
            assert(false) : "We didn't add decks";
        }
        for (DeckStrategy strat: decks) {
            System.out.println(strat.getName());
        }
    }

    @Test
    public void parameterdeckstrategytest() throws IOException {
        JsonDeckStrategy jsondeck = new JsonDeckStrategy();
        DeckStrategy deckstrategy = jsondeck.createDeckStrategy("src\\main\\java\\forge\\game\\research\\decision\\Decks\\MonoRed.json");
        for(String paramkey : deckstrategy.getParameterKeys()){
            System.out.println(paramkey + ":" + deckstrategy.getParameter(paramkey));
        }
    }
}
