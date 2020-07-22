package forge.game.research;

import forge.GuiDesktop;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import forge.GuiDesktop.*;
public class SimulationTesting {
    private static boolean initialized = false;
    private final ArrayList<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
    private final GameRules rules = new GameRules(GameType.Draft);
    private final String title = "Test Match";
    @Test
    public void testthismethodpls(){
            //GameRules grules = new GameRules(GameType.Draft);
            ArrayList<RegisteredPlayer> testplayers = new ArrayList<RegisteredPlayer>();
            RegisteredPlayer registeredPlayer1 = new RegisteredPlayer(new Deck());
            testplayers.add(registeredPlayer1);
            RegisteredPlayer registeredPlayer2 = new RegisteredPlayer(new Deck());
            testplayers.add(registeredPlayer2);
            Match match = new Match(rules,players,title);
            Game game = match.createGame();
    }
}
