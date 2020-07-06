package forge.game.research.decision.infosupport;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.player.Player;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class PayForCosts {
    public static final String RED = "R";
    public static final String GREEN = "G";
    public static final String BLUE = "U";
    public static final String BLACK = "B";
    public static final String WHITE = "W";

    private ArrayList<Card> needed;
    private ArrayList<Card> lands;

    public PayForCosts() {

    }

    public void setManaSources(Player payer, ArrayList<SpellAbility> spells) {
        lands = new ArrayList<>();
        needed = new ArrayList<>();
        for (Card c: payer.getZone(ZoneType.Battlefield)) {
            if(!c.isTapped() && c.isLand()) {
                lands.add(c);
            }
        }
        for (SpellAbility sa: spells) {
            if (!(sa instanceof LandAbility)) {
                for (CostPart parts: CostAdjustment.adjust(sa.getPayCosts(), sa).getCostParts()) {
                    if (parts instanceof CostPartMana) {
                        ManaCost toPay = ((CostPartMana) parts).getMana();
                        for (ManaCostShard shard: toPay) {
                            //Do Generic costs at the end.
                            if (shard.isWhite()){
                                reserve(payer, WHITE);
                            } else if (shard.isBlue()){
                                reserve(payer, BLUE);
                            } else if (shard.isBlack()){
                                reserve(payer, BLACK);
                            } else if (shard.isRed()){
                                reserve(payer, RED);
                            } else if (shard.isGreen()){
                                reserve(payer, GREEN);
                            }
                        }
                    }
                }
            }
        }


    }

    /**
     * Pays for the mana portion of a spell. Other costs (additional casting costs)
     * are not payable by our Ai.
     * Only taps lands for now
     * @return Whether the paying was successful.
     */
    public boolean payTheManaCost(Player payer, List<CostPart> costs, SpellAbility sp,
                                  ArrayList<SpellAbility> toplay) {
        setManaSources(payer, toplay);
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
        for (Card c: lands) {
            if (done) {
                return true;
            }
            for (SpellAbility sa : c.getManaAbilities()) {
                String type = sa.getMapParams().get("Produced");
                if (!done && type.contains(color) && !needed.contains(c)) {
                    done = true;
                    needed.add(c);
                    c.tap();
                }
            }
        }
        return done;
    }

    public void reserve(Player p, String color) {
        boolean done = false;
        for (Card c: lands) //Try to find a basic land first
           if (c.isBasicLand()) {
               for (SpellAbility sa : c.getManaAbilities()) {
                   String type = sa.getMapParams().get("Produced");
                   if (type.contains(color)&& !needed.contains(c)) {
                       needed.add(c);
                       return;
                   }
               }
           }

        //Then just try all lands;
        for (Card c: lands) {
            for (SpellAbility sa : c.getManaAbilities()) {
                String type = sa.getMapParams().get("Produced");
                if (type.contains(color) && !needed.contains(c)) {
                    needed.add(c);
                    return;
                }
            }
        }
    }

    public boolean payForGeneric(Player p, int cost) {

        for (int i=0; i<cost; i++) {
            boolean done = false;
            for (Card c: lands) {
                if (!done && !needed.contains(c)) {
                    done = true;
                    needed.add(c);
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
