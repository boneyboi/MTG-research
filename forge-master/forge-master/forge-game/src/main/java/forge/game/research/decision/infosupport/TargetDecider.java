package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;

public class TargetDecider {

    public TargetDecider() {

    }


    public void assignTargets(SpellAbility sa) {
        sa.resetTargets();
        TargetChoices targets = new TargetChoices();
        for (Player player: sa.getHostCard().getController().getGame().getPlayers()){
            if (sa.canAddMoreTarget() && !sa.isTargetNumberValid()) {
                if (sa.canTarget(player)) {
                    targets.add(player);
                    sa.setTargets(targets);
                } else {
                    //TODO: Add other zones too maybe
                    for (Card card: player.getZone(ZoneType.Battlefield)) {
                        if (sa.canAddMoreTarget()
                                && !sa.isTargetNumberValid()
                                && sa.canTarget(card)) {
                            targets.add(card);
                            sa.setTargets(targets);
                        }
                    }
                }
            }
        }
    }
}
