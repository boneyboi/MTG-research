package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.decision.infosupport.removal.RemovalChecker;
import forge.game.research.decision.infosupport.removal.RemovalList;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class TargetDecider {

    public TargetDecider() {

    }


    public void assignTargets(SpellAbility sa) {
        Player controller = sa.getHostCard().getController();
        sa.resetTargets();
        if (sa.isTargetNumberValid()) {
            return;
        }
        TargetChoices targets = new TargetChoices();
        RemovalChecker rc = new RemovalChecker();
        RemovalList list = new RemovalList();

        if (!sa.isTargetNumberValid()) {
            for (Player p: controller.getGame().getPlayers()) {
                if (sa.canTarget(p) && sa.canAddMoreTarget()) {
                    targets.add(p);
                    sa.setTargets(targets);
                }
            }
        }
        ArrayList<Card> options = new ArrayList<>();
        if (!sa.isTargetNumberValid()) {
            if (!rc.shouldTargetOthers(sa.getHostCard())){
                for (Card c: controller.getZone(ZoneType.Battlefield)) {
                    options.add(c);
                }
            } else {
                for (Card c: controller.getOpponents().get(0).getZone(ZoneType.Battlefield)) {
                    options.add(c);
                }
            }
            Card chosen = list.getBiggestThreat(options);
            if(sa.canTarget(chosen)) {
                targets.add(chosen);
                sa.setTargets(targets);
            }
        }
    }
}
