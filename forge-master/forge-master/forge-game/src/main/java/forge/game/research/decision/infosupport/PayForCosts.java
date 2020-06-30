package forge.game.research.decision.infosupport;

import forge.game.cost.CostPart;
import forge.game.player.Player;

import java.util.List;

public class PayForCosts {
    public PayForCosts() {

    }

    /**
     * Pays for the mana portion of a spell. Other costs are not payable by our Ai.
     * @return Whether the paying was successful.
     */
    public boolean payTheManaCost(Player payer, List<CostPart> costs) {
        return true;
    }
}
