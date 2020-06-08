/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.Map;

/**
 * <p>
 * Trigger_Taps class.
 * </p>
 * 
 * @author Forge
 */
public class TriggerTaps extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Taps.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            a boolean
     */
    public TriggerTaps(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final Card tapper = (Card) runParams.get(AbilityKey.Card);

        if (hasParam("ValidCard")) {
            if (!tapper.isValid(getParam("ValidCard").split(","), getHostCard().getController(),
                    getHostCard(), null)) {
                return false;
            }
        }
        if (hasParam("Attacker")) {
            if ("True".equalsIgnoreCase(getParam("Attacker"))) {
                if (!(Boolean) runParams.get(AbilityKey.Attacker)) {
                    return false;
                }
            } else if ("False".equalsIgnoreCase(getParam("Attacker"))) {
                if ((Boolean) runParams.get(AbilityKey.Attacker)) {
                    return false;
                }
            }
        }

        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Card);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblTapped")).append(": ").append(sa.getTriggeringObject(AbilityKey.Card));
        return sb.toString();
    }

}
