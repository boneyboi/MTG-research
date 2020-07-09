/**
 * The strategies for decks (esp. our black/white lifelink, monored calvacade, and black/blue surveil)
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 23 June 2020
 */

package forge.game.research.decision.strategy;


import forge.game.research.decision.strategy.template.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class DeckStrategies {
    public static ArrayList<DeckStrategy> decks = new ArrayList<>();


    public DeckStrategies(){
        deckStratInit();
    }

    public void deckStratInit(){
        JsonDeckStrategy builder = new JsonDeckStrategy();
        File path = new File("../..");
        System.out.println(path.getAbsolutePath());
        File folder = new File("..\\..\\forge-master\\forge-game\\src\\main\\java\\forge\\game\\research\\decision\\Decks");
        Path dir = folder.toPath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)){
            for (Path file: stream) {
                DeckStrategy temp = builder.createDeckStrategy(file.toString());
                decks.add(temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<DeckStrategy> getDecks() {
        return decks;
    }

    public int getNumDecks() {
        return decks.size();
    }

    public DeckStrategy getDeckNamed(String name) {
        for (DeckStrategy strat: decks) {
            if (strat.getName().equals(name)) {
                return strat;
            }
        }
        return null;
    }


}
