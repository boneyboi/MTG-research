/**
 * Returns which card to play using a 'voting' system
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 19 June 2020
 */

package forge.game.research.decision.infosupport;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.research.decision.strategy.Strategy;
import forge.game.research.decision.strategy.StrategyNode;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;

public class BallotBox {
    public Player controller;
    public ArrayList<SpellAbility> nonlands;
    public ArrayList<Card> lands;

    public BallotBox(Player p){
        controller = p;
    }

    public void getOptions() {
        ViablePlays vp = new ViablePlays(controller);
        nonlands = vp.getNonlandPlays();
        lands = vp.getLandPlays();
    }

    /**
     * Return the card that is voted on
     * use this space to describe how a card is voted on
     * @return
     */
    public Card votedCard(){
        return null;
    }

    /**
     * Go through a strategy and get the last playable node
     * @param strategy
     */
    public StrategyNode getViableNode(Strategy strategy){

        return null;
    }
}
