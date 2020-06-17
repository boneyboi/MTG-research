/**
 * Class that returns the decision when the computer asks for one
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since June 19 2020
 */
package forge.game.research.decision;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Facade {

    public Facade() {
    }

    /**
     * Used when deciding attackers
     * @param possAttackers : list
     * @param possBlockers : list
     * @return list of creatures
     * TODO: change void to 'list or arraylist', fill out body of method, edit docstring if necessary
     */
    public void getAttackers (ArrayList possAttackers, ArrayList possBlockers) {
    }

    /**
     * Used when deciding blockers
     * @param possAttackers : list
     * @param possBlockers : list
     * @return map
     * TODO: change void to 'map', create map, fill out body of method, edit docstring if necessary
     */
    public void getBlockers (ArrayList possAttackers, ArrayList possBlockers) {
    }

     /**
     * Used when deciding what card or ability to play
     * Get decision (param: List of options/SpellAbilities)
     * (return Spell Ability)
     */
     public void getDecision (ArrayList options) {

     }

     /**
     * Used when deciding to mulligan and what cards to keep if we do.
     * Mulligan (param: Hand, Mull #)
     * (return List of cards to put back, or null if we want to mulligan)
     *
     * Used when we have to discard a card but can choose which card to discard.
     * Forced Discard (param: Hand)
     * (return card)
     *
     * Used when we are forced to sacrifice a permanent.
     * Forced Sacrifice (param: List of cards that can be sacrificed)
     * (return card)
     *
     * Used when our creature is blocked by multiple creatures, and we have to assign damage.
     * Damage on Double block (param: Attacker power, list of defenders)
     * (return List of integers)
     *
    */

}
