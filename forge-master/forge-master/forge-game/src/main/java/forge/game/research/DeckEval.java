package forge.game.research;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DeckEval extends ZoneEvaluator{

    public DeckEval(Player p) {
        super(ZoneType.Library, p, 1);
    }

}
