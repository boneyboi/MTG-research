package forge.game.research;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

import forge.game.player.Player;

import forge.game.phase.PhaseHandler;

public class PlayCards {

    private Player MplayerPriority;
    private Player turn;

    public PlayCards(Player playerPriority) {
        MplayerPriority = playerPriority;
    }

    public ArrayList playMethod() {
        ArrayList toplay = null;
        if (MplayerPriority.getName().equals("Ai")){
            for (Card card : MplayerPriority.getCardsIn(ZoneType.Hand)) {
                if (card.getName().equals("Memnite")) {
                    toplay.add(card.getFirstSpellAbility());
                }
            }
        }
        return toplay;
    }
}
