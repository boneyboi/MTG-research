/**
 * Determines what cards can be played with the mana available
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;

public class ViablePlays {
    ArrayList<SpellAbility> plays = new ArrayList<SpellAbility>();
    Player controller = null;
    ArrayList<Integer> manapool;
    public int whiteMana;
    public int blueMana;
    public int blackMana;
    public int redMana;
    public int greenMana;



    //TODO: add public final string constants for land string literals like "R" or "B"
    public ViablePlays(Player p) {
        controller = p;
    }



    public ArrayList<SpellAbility> getPlays() {
        emptyOptions();
        buildOptions();
        return plays;
    }

    public boolean areColorsAvaliable(SpellAbility sa) {
        int[] shardsNeeded = sa.getPayCosts().getTotalMana().getColorShardCounts();
        if (shardsNeeded[0] > whiteMana ||
                shardsNeeded[1] > blueMana ||
                shardsNeeded[2] > blackMana ||
                shardsNeeded[3] > redMana ||
                shardsNeeded[4] > greenMana) {
            return false;
        }
        return true;
    }


    public void addZoneOptions(ZoneType z) {
        for (Card c: controller.getZone(z)) {
            for (SpellAbility sa: c.getNonManaAbilities()) {
                if (sa.getPayCosts().getTotalMana().getCMC() <= (int) manapool.get(0) &&
                        sa.canPlay() &&
                        areColorsAvaliable(sa)) {
                    plays.add(sa);
                }
            }
        }
    }

    public void buildOptions() {
        ManaEvaluation manaOptions = new ManaEvaluation(controller);
        manapool = manaOptions.getReturnValues();

        whiteMana = manapool.get(1);
        blueMana = manapool.get(2);
        blackMana = manapool.get(3);
        redMana = manapool.get(4);
        greenMana = manapool.get(5);

        addZoneOptions(ZoneType.Hand);
        addZoneOptions(ZoneType.Battlefield);
        addZoneOptions(ZoneType.Graveyard);
        addZoneOptions(ZoneType.Exile);
        //TODO: addLandOptions();
    }

    public void emptyOptions() {
        plays = new ArrayList<SpellAbility>();
    }

}
