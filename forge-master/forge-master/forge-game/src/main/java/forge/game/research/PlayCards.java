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

    //the player whose turn it is
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
     * Uses viable plays in combination with deck strategies to make Ai play specific cards from their hand
     * @param controller
     * @return toplay (which has a list of cards) or null if the player's name is not Ai
     */
    public ArrayList playChosenFromHand(Player controller) {
        BallotBox voter = new BallotBox(controller);
        //TODO: Move this to beginning of game
        DeckStrategies trial = new DeckStrategies();
        ArrayList toplay = new ArrayList<SpellAbility>();
        ViablePlays vp = new ViablePlays(controller);
        SpellAbility chosen = voter.votedCard(DeckStrategies.lifelinkstrats);
        if (chosen != null) {
            toplay.add(chosen);
            return toplay;
        }
        else {
            return null;
        }

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
     * Method that allows for the playing of lands, then if a land has already been played for the turn or cannot
     * be played that turn, can return a different card/spell ability
     * @return toplay -> land that is to be played
     * @return playChosenFromHand(controller) -> spell ability list if a land has or cannot be played
     */
    public ArrayList playLand() {
        ArrayList toplay = new ArrayList<SpellAbility>();
        SpellAbility sa;

        //checks if player has played a land and if a player is able to play a land at this time
        if (controller.getLandsPlayedThisTurn() < controller.getMaxLandPlays()
                && controller.canCastSorcery()) {
            //iterates through hand, checks if land is present, then returns that land
            for (Card c: controller.getZone(ZoneType.Hand)) {
                if (c.isLand()) {
                    sa = new LandAbility(c, controller, null);
                    toplay.add(sa);
                    return toplay;
                }
            }
        }
        //else calls playChosenFromHand() to decide on another card
        return playChosenFromHand(controller);
    }

    /**
     * Initial method of getting Ai to play specific cards by returning a list of spell abilities
     * @return toplay (which has a list of cards) or null if the player's name is not Ai
     */
    private ArrayList playMethod() {
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
