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
    public SpellAbility playChosenFromHand(Player controller, ArrayList<SpellAbility> playing) {
        BallotBox voter = new BallotBox(controller);
        SpellAbility chosen = voter.votedCard(DeckStrategies.lifelinkstrats, false, playing);
        return chosen;

    }

    /**
     * Method that allows for the playing of lands, then if a land has already been played for the turn or cannot
     * be played that turn, can return a different card/spell ability
     * @return toplay -> land that is to be played
     * @return playChosenFromHand(controller) -> spell ability list if a land has or cannot be played
     */
    public SpellAbility playLand(ArrayList<SpellAbility> playing) {
        BallotBox voter = new BallotBox(controller);
        SpellAbility sa;
        Card land = null;

        land = voter.choseLand(DeckStrategies.lifelinkstrats, playing);

        if (land != null) {
            sa = new LandAbility(land, controller, null);
            return sa;
        }


        //else calls playChosenFromHand() to decide on another card
        return playChosenFromHand(controller, playing);
    }
}
