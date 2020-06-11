package forge.game.research;

import forge.game.player.Player;
import forge.game.research.Zone.*;

import java.util.ArrayList;
import java.util.List;

public class PlayerStateEval{

    public final Player player;
    public PlayerStateEval(Player p) {
        player = p;
    }

    /**
     * Calculates the values of all zones a player controls
     * @return List of values for Battlefield, Exile, Graveyard, Hand, and Library,
     * in that abc order.
     */
    public List<Double> evaluate() {
        ZoneEvaluator Zone = new BattlefieldEval(player);
        ArrayList<Double> values = new ArrayList<Double>();
        values.add(Zone.evaluateZone());
        Zone = new ExileEval(player);
        values.add(Zone.evaluateZone());
        Zone = new GraveyardEval(player);
        values.add(Zone.evaluateZone());
        Zone = new HandEval(player);
        values.add(Zone.evaluateZone());
        Zone = new DeckEval(player);
        values.add(Zone.evaluateZone());
        return values;
    }
}
