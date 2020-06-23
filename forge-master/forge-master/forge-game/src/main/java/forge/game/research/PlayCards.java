/**
 * Class that allows for our a.i. to play specific cards, including  instructions
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 23, 2020
 */

package forge.game.research;

import forge.game.card.Card;
import forge.game.research.decision.infosupport.BallotBox;
import forge.game.research.decision.infosupport.ViablePlays;
import forge.game.research.decision.strategy.DeckStrategies;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.research.decision.strategy.template.CardTemplate;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import java.util.ArrayList;
import forge.game.player.Player;

public class PlayCards {

    private Player controller;

    /**
     * Allows for the class to obtain whose priority it is currently
     * @param playerPriority
     * @return toplay (which has a list of cards) or null if the player's name is not Ai
     */
    public PlayCards(Player playerPriority) {
        controller = playerPriority;
    }

    /**
     * Uses viable plays in combination with deck strategies to make the
     * @param controller
     * @return toplay (which has a list of cards) or null if the player's name is not Ai
     */
    public ArrayList playChosenFromHand(Player controller) {
        BallotBox voter = new BallotBox(controller);
        DeckStrategies trial = new DeckStrategies();
        ArrayList toplay = new ArrayList<SpellAbility>();
        ViablePlays vp = new ViablePlays(controller);
        StrategyNode chosen = voter.votedCard(DeckStrategies.monoredStrats);

        //Find card from node
        for (CardTemplate template: chosen.getCards()) {
            for (SpellAbility option : vp.getNonlandPlays()) {
                if (template.matches(option)) {
                    toplay.add(option);
                    return toplay;
                }
            }
        }
        return null;

    }

    /**
     * Allows for 'Ai' to play cards only from certain options/viable plays
     * @return toplay (which has a list of cards) or null if the player's name is not Ai
     */
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

    /**
     *
     * @return
     */
    public ArrayList playLand() {
        ArrayList toplay = new ArrayList<SpellAbility>();
        SpellAbility sa;
        if (controller.getLandsPlayedThisTurn() < controller.getMaxLandPlays()
                && controller.canCastSorcery()) {
            for (Card c: controller.getZone(ZoneType.Hand)) {
                if (c.isLand()) {
                    sa = new LandAbility(c, controller, null);
                    toplay.add(sa);
                    return toplay;
                }
            }
        }
        return playChosenFromHand(controller);
    }

    /**
     *
     * @return toplay (which has a list of cards) or null if the player's name is not Ai
     */
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
