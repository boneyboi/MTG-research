package forge.game.research;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

import forge.game.player.Player;

import forge.game.phase.PhaseHandler;

public class PlayCards {

    private Player controller;
    private Player turn;

    public PlayCards(Player playerPriority) {
        controller = playerPriority;
    }

    public ArrayList playMethod() {
        ArrayList toplay = new ArrayList<SpellAbility>();
        if (controller.getName().equals("Ai")){
            for (Card card : controller.getCardsIn(ZoneType.Hand)) {
                if (card.getName().equals("Memnite")) {
                    SpellAbility sa = card.getFirstSpellAbility();
                    if (sa.canPlay()) {
                        toplay.add(sa);
                        return toplay;
                    }

                }
            }
        }
        return null;
    }
}
