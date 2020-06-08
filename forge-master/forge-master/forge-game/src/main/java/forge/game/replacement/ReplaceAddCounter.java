package forge.game.replacement;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceAddCounter extends ReplacementEffect {

    /**
     * 
     * ReplaceProduceMana.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceAddCounter(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (((int) runParams.get(AbilityKey.CounterNum)) <= 0) {
            return false;
        }

        if (hasParam("EffectOnly")) {
            final Boolean effectOnly = (Boolean) runParams.get(AbilityKey.EffectOnly);
            if (!effectOnly) {
                return false;
            }
        }

        if (hasParam("ValidCard")) {
            Object o = runParams.get(AbilityKey.Affected);
            if (!(o instanceof Card)) {
                return false;
            }
            if (!matchesValid(o, getParam("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        } else if (hasParam("ValidPlayer")) {
            Object o = runParams.get(AbilityKey.Affected);
            if (!(o instanceof Player)) {
                return false;
            }
            if (!matchesValid(o, getParam("ValidPlayer").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (hasParam("ValidCounterType")) {
            String type = getParam("ValidCounterType");
            if (CounterType.getType(type) != runParams.get(AbilityKey.CounterType)) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.CounterNum, runParams.get(AbilityKey.CounterNum));
        sa.setReplacingObject(AbilityKey.CounterType, ((CounterType) runParams.get(AbilityKey.CounterType)).getName());
        Object o = runParams.get(AbilityKey.Affected);
        if (o instanceof Card) {
            sa.setReplacingObject(AbilityKey.Card, o);
        } else if (o instanceof Player) {
            sa.setReplacingObject(AbilityKey.Player, o);
        }
    }

}
