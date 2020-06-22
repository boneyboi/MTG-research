package forge.game.research;

import forge.game.card.Card;
import forge.game.research.decision.infosupport.ViablePlays;
import forge.game.spellability.LandAbility;
import forge.game.spellability.Spell;
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

    public ArrayList playFromOptions() {
        ArrayList toplay = new ArrayList<SpellAbility>();
        ViablePlays vp = new ViablePlays(controller);
        if (!vp.getNonlandPlays().isEmpty()) {
            toplay.add(vp.getNonlandPlays().get(0));
            return toplay;
        } else {
            return null;
        }

    }

    public ArrayList playLand() {
        ArrayList toplay = new ArrayList<SpellAbility>();
        SpellAbility sa;
        if (controller.getLandsPlayedThisTurn() < controller.getMaxLandPlays() && controller.canCastSorcery()) {
            for (Card c: controller.getZone(ZoneType.Hand)) {
                if (c.isLand()) {
                    sa = new LandAbility(c, controller, null);
                    toplay.add(sa);
                    return toplay;
                }
            }
        }
        return playFromOptions();
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
