package forge.game.research.decision;

public class Facade {

    /**
     * Used when deciding attackers
     * Get Attackers (param: List of possible attackers, List of possible blockers)
     * (return list of creatures)
     *
     * Used when deciding blockers
     * Get Blockers (param: List of attackers, List of possible blockers)
     * (return as map)
     *
     * Used when deciding what card or ability to play
     * Get decision (param: List of options/SpellAbilities)
     * (return Spell Ability)
     *
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
