/**
 * Decides on when and how to mulligan
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 30 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.research.zone.DeckEval;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class Mulligan {

    Player controller;

    //number of times Ai have mulled
    private int timeMull = 0;

    //number of times Ai should mull
    public static final int STOPMULL = 3;

    public static final int NONLANDSNEEDED = 2;
    public static final int STARTINGHANDSIZE = 7;

    public Mulligan () {
    }

    /**
     *
     * @param mullingPlayer
     * @param cardsNeeded
     * @return
     */
    public CardCollection returnCards(Player mullingPlayer, int cardsNeeded) {
        CardCollection returning = new CardCollection();
        ArrayList<Card> hand = new ArrayList<>();
        ArrayList<Card> keeping = new ArrayList<>();
        ArrayList<Card> compare = new ArrayList<>();
        HandAssessment decider = new HandAssessment(mullingPlayer, mullingPlayer.getFacade().getPlan());
        for (Card c: mullingPlayer.getZone(ZoneType.Hand)) {
            hand.add(c);
        }
        for (List<Card> temp : new CombinationIterable<>(hand))  {
            if (temp.size() == (STARTINGHANDSIZE - cardsNeeded)) {
                ArrayList<Card> combination = new ArrayList<>();
                combination.addAll(temp);
                if (keeping.isEmpty()) {
                    keeping = combination;
                } else {
                    keeping = decider.compareHands(keeping, combination);
                }
            }
        }

        for (Card c: hand) {
            if (!keeping.contains(c)) {
                returning.add(c);
            }
        }
        System.out.print(returning);
        return returning;
    }

    /**
     *
     * @param player
     * @return boolean
     */
    public boolean shouldMull (Player player) {
        //controls the max number of times Ai should mulligan
        HandAssessment decider = new HandAssessment(player, player.getFacade().getPlan());
        if(timeMull == STOPMULL) {
            return false;
        } else {
            timeMull++;
            return !(decider.judgeHand());
        }
    }

    /**
     * Allows outside classes to get the number of times Ai has made
     * @return timeMull
     */
    public int getTimeMull() {
        return timeMull;
    }

    /**
     * Helper method that determines if a player's library does not have lands
     * @param player
     * @return
     */
    private boolean doesNotHaveLands(Player player) {
        for (Card card: player.getCardsIn(ZoneType.Library)) {
            if (card.isLand()) {
              return false;
            }
        }
        return true;
    }
}
