package forge.game.research.decision.infosupport;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class PayForCosts {
    public static final String RED = "R";
    public static final String GREEN = "G";
    public static final String BLUE = "U";
    public static final String BLACK = "B";
    public static final String WHITE = "W";
    public PayForCosts() {

    }

    /**
     * Pays for the mana portion of a spell. Other costs (additional casting costs)
     * are not payable by our Ai.
     * Only taps lands for now
     * @return Whether the paying was successful.
     */
    public boolean payTheManaCost(Player payer, List<CostPart> costs, SpellAbility sp) {
        TargetDecider td = new TargetDecider();
        td.assignTargets(sp);
        for (CostPart parts: costs) {
            if (parts instanceof CostPartMana){
                ManaCost toPay = ((CostPartMana) parts).getMana();
                for (ManaCostShard shard: toPay) {
                    //Do Generic costs at the end.
                    if (shard.isWhite()){
                        if (!payForShard(payer, WHITE)) {
                            return false;
                        }
                    } else if (shard.isBlue()){
                        if (!payForShard(payer, BLUE)) {
                            return false;
                        }
                    } else if (shard.isBlack()){
                        if (!payForShard(payer, BLACK)) {
                            return false;
                        }
                    } else if (shard.isRed()){
                        if (!payForShard(payer, RED)) {
                            return false;
                        }
                    } else if (shard.isGreen()){
                        if (!payForShard(payer, GREEN)) {
                            return false;
                        }
                    }
                }
                return (payForGeneric(payer, toPay.getGenericCost()));
            }
        }
        return true;
    }


    /**
     * Look for
     */
    public boolean payForShard(Player p, String color) {
        boolean done = false;
        for (Card c: p.getZone(ZoneType.Battlefield)) {
            if (done) {
                return true;
            }
            if (!c.isTapped() && c.isLand()) {
                for (SpellAbility sa : c.getManaAbilities()) {
                    String type = sa.getMapParams().get("Produced");
                    if (!done && type.contains(color)) {
                        done = true;
                        c.tap();
                    }
                }
            }
        }
        return false;
    }

    public boolean payForGeneric(Player p, int cost) {

        for (int i=0; i<cost; i++) {
            boolean done = false;
            for (Card c: p.getZone(ZoneType.Battlefield)) {
                if (!done
                        && !c.getManaAbilities().isEmpty()
                        && !c.isTapped()
                        && c.isLand()) {
                    done = true;
                    c.tap();
                }
            }
            if (!done) {
                return false;
            }
        }
        return true;
    }
}
