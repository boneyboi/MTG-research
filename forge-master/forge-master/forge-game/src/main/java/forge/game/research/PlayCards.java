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
        boolean memnite = false;
        boolean hopethisworks = false;
        ArrayList toplay = null;

        if (MplayerPriority.equals("Ai")){
            hopethisworks = true;
            for (Card card : MplayerPriority.getCardsIn(ZoneType.Hand)) {
                if (card.getName().equals("Memnite")) {
                    memnite = true;
                    toplay.add(card);
                }
            }
        }
        boolean diditwork = hopethisworks;
        boolean memniteforever = memnite;
        return toplay;
    }
}
