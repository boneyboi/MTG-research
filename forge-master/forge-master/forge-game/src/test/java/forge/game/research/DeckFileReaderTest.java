package forge.game.research;

import com.google.gson.Gson;
import forge.game.research.decision.strategy.DeckStrategy;
import forge.game.research.decision.strategy.JsonDeckStrategy;
import forge.game.research.decision.strategy.Strategy;
import org.junit.Test;

import java.io.IOException;

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
        DeckStrategy deckstrategy = jsondeck.createDeckStrategy("C:\\Users\\deric\\Desktop\\MTG-research\\forge-master\\forge-master\\forge-game\\src\\main\\java\\forge\\game\\research\\decision\\Decks\\MonoRed.json");
        System.out.println(deckstrategy.getStrategies().get(0).getName());
    }
}
