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
package forge.game.ability;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.card.CardStateName;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.cost.Cost;
import forge.game.spellability.*;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * AbilityFactory class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory {

    static final List<String> additionalAbilityKeys = Lists.newArrayList(
            "WinSubAbility", "OtherwiseSubAbility", // Clash
            "ChooseNumberSubAbility", "Lowest", "Highest", // ChooseNumber
            "HeadsSubAbility", "TailsSubAbility", "LoseSubAbility", // FlipCoin
            "TrueSubAbility", "FalseSubAbility", // Branch
            "ChosenPile", "UnchosenPile", // MultiplePiles & TwoPiles
            "RepeatSubAbility", // Repeat & RepeatEach
            "Execute", // DelayedTrigger
            "FallbackAbility", // Complex Unless costs which can be unpayable
            "ChooseSubAbility", // Can choose a player via ChoosePlayer
            "CantChooseSubAbility", // Can't choose a player via ChoosePlayer
            "AnimateSubAbility" // For ChangeZone Effects to Animate before ETB
        );

    public enum AbilityRecordType {
        Ability("AB"),
        Spell("SP"),
        StaticAbility("ST"),
        SubAbility("DB");
        
        private final String prefix;
        AbilityRecordType(String prefix) {
            this.prefix = prefix;
        }
        public String getPrefix() {
            return prefix;
        }
        
        public SpellAbility buildSpellAbility(ApiType api, Card hostCard, Cost abCost, TargetRestrictions abTgt, Map<String, String> mapParams ) {
            switch(this) {
                case Ability: return new AbilityApiBased(api, hostCard, abCost, abTgt, mapParams);
                case Spell: return new SpellApiBased(api, hostCard, abCost, abTgt, mapParams);
                case StaticAbility: return new StaticAbilityApiBased(api, hostCard, abCost, abTgt, mapParams);
                case SubAbility: return new AbilitySub(api, hostCard, abTgt, mapParams);
            }
            return null; // exception here would be fine!
        }
        
        public ApiType getApiTypeOf(Map<String, String> abParams) {
            return ApiType.smartValueOf(abParams.get(this.getPrefix()));
        }
        
        public static AbilityRecordType getRecordType(Map<String, String> abParams) {
            if (abParams.containsKey(AbilityRecordType.Ability.getPrefix())) {
                return AbilityRecordType.Ability;
            } else if (abParams.containsKey(AbilityRecordType.Spell.getPrefix())) {
                return AbilityRecordType.Spell;
            } else if (abParams.containsKey(AbilityRecordType.StaticAbility.getPrefix())) {
                return AbilityRecordType.StaticAbility;
            } else if (abParams.containsKey(AbilityRecordType.SubAbility.getPrefix())) {
                return AbilityRecordType.SubAbility;
            } else {
                return null;
            }
        }
    }
    
    public static final SpellAbility getAbility(final String abString, final Card card) {
        return getAbility(abString, card.getCurrentState(), null);
    }
    /**
     * <p>
     * getAbility.
     * </p>
     * 
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public static final SpellAbility getAbility(final String abString, final CardState state) {
        return getAbility(abString, state, null);
    }
    
    private static final SpellAbility getAbility(final String abString, final CardState state, final SpellAbility parent) {
        Map<String, String> mapParams;
        try {
            mapParams = AbilityFactory.getMapParams(abString);
        }
        catch (RuntimeException ex) {
            throw new RuntimeException(state.getName() + ": " + ex.getMessage());
        }
        // parse universal parameters
        AbilityRecordType type = AbilityRecordType.getRecordType(mapParams);
        if (null == type) {
            String source = state.getName().isEmpty() ? abString : state.getName();
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + source + ": " + abString);
        }
        try {
            return getAbility(mapParams, type, state, parent);
        } catch (Error | Exception ex) {
            String msg = "AbilityFactory:getAbility: crash when trying to create ability ";
            Sentry.getContext().recordBreadcrumb(
                new BreadcrumbBuilder().setMessage(msg)
                .withData("Card", state.getName()).withData("Ability", abString).build()
            );
            throw new RuntimeException(msg + " of card: " + state.getName(), ex);
        }
    }
    
    public static final SpellAbility getAbility(final Card hostCard, final String svar) {
        return getAbility(hostCard.getCurrentState(), svar, null);
    }

    public static final SpellAbility getAbility(final CardState state, final String svar) {
        return getAbility(state, svar, null);
    }
    
    private static final SpellAbility getAbility(final CardState state, final String svar, final SpellAbility parent) {
        if (!state.hasSVar(svar)) {
            String source = state.getCard().getName();
            throw new RuntimeException("AbilityFactory : getAbility -- " + source +  " has no SVar: " + svar);
        } else {
            return getAbility(state.getSVar(svar), state, parent);
        }
    }
    
    public static final SpellAbility getAbility(final Map<String, String> mapParams, AbilityRecordType type, final Card card, final SpellAbility parent) {
        return getAbility(mapParams, type, card.getCurrentState(), parent);
    }

    public static final SpellAbility getAbility(final Map<String, String> mapParams, AbilityRecordType type, final CardState state, final SpellAbility parent) {
        return getAbility(type, type.getApiTypeOf(mapParams), mapParams, parseAbilityCost(state, mapParams, type), state, parent);
    }


    public static Cost parseAbilityCost(final CardState state, Map<String, String> mapParams, AbilityRecordType type) {
        Cost abCost = null;
        if (type != AbilityRecordType.SubAbility) {
            String cost = mapParams.get("Cost");
            if (cost == null) {
                throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + state.getName());
            }
            abCost = new Cost(cost, type == AbilityRecordType.Ability);
        }
        return abCost;
    }

    public static final SpellAbility getAbility(AbilityRecordType type, ApiType api, Map<String, String> mapParams,
            Cost abCost,final Card card, final SpellAbility parent) {
        return getAbility(type, api, mapParams, abCost, card.getCurrentState(), parent);
    }
    
    public static final SpellAbility getAbility(AbilityRecordType type, ApiType api, Map<String, String> mapParams,
            Cost abCost,final CardState state, final SpellAbility parent) {
        final Card hostCard = state.getCard();
        TargetRestrictions abTgt = mapParams.containsKey("ValidTgts") ? readTarget(mapParams) : null;

        if (api == ApiType.CopySpellAbility || api == ApiType.Counter || api == ApiType.ChangeTargets || api == ApiType.ControlSpell) {
            // Since all "CopySpell" ABs copy things on the Stack no need for it to be everywhere
            // Since all "Counter" or "ChangeTargets" abilities only target the Stack Zone
            // No need to have each of those scripts have that info
            if (abTgt != null) {
                abTgt.setZone(ZoneType.Stack);
            }
        }

        else if (api == ApiType.PermanentCreature || api == ApiType.PermanentNoncreature) {
            // If API is a permanent type, and creating AF Spell
            // Clear out the auto created SpellPemanent spell
            if (type == AbilityRecordType.Spell
                    && !mapParams.containsKey("SubAbility") && !mapParams.containsKey("NonBasicSpell")) {
                hostCard.clearFirstSpell();
            }
        }


        SpellAbility spellAbility = type.buildSpellAbility(api, hostCard, abCost, abTgt, mapParams);


        if (spellAbility == null) {
            final StringBuilder msg = new StringBuilder();
            msg.append("AbilityFactory : SpellAbility was not created for ");
            msg.append(state.getName());
            msg.append(". Looking for API: ").append(api);
            throw new RuntimeException(msg.toString());
        }

        // need to set Parent Early
        if (parent != null && spellAbility instanceof AbilitySub) { 
            ((AbilitySub)spellAbility).setParent(parent);
        }

        // *********************************************
        // set universal properties of the SpellAbility

        if (mapParams.containsKey("References")) {
            for (String svar : mapParams.get("References").split(",")) {
                spellAbility.setSVar(svar, state.getSVar(svar));
            }
        }

        if (api == ApiType.DelayedTrigger && mapParams.containsKey("Execute")) {
            spellAbility.setSVar(mapParams.get("Execute"), state.getSVar(mapParams.get("Execute")));
        }

        if (mapParams.containsKey("PreventionSubAbility")) {
            spellAbility.setSVar(mapParams.get("PreventionSubAbility"), state.getSVar(mapParams.get("PreventionSubAbility")));
        }

        if (mapParams.containsKey("SubAbility")) {
            final String name = mapParams.get("SubAbility");
            spellAbility.setSubAbility(getSubAbility(state, name, spellAbility));
        }

        for (final String key : additionalAbilityKeys) {
            if (mapParams.containsKey(key) && spellAbility.getAdditionalAbility(key) == null) {
                spellAbility.setAdditionalAbility(key, getSubAbility(state, mapParams.get(key), spellAbility));
            }
        }

        if (api == ApiType.Charm  || api == ApiType.GenericChoice || api == ApiType.AssignGroup) {
            final String key = "Choices";
            if (mapParams.containsKey(key)) {
                List<String> names = Lists.newArrayList(mapParams.get(key).split(","));
                final SpellAbility sap = spellAbility;
                spellAbility.setAdditionalAbilityList(key, Lists.transform(names, new Function<String, AbilitySub>() {
                    @Override
                    public AbilitySub apply(String input) {
                        return getSubAbility(state, input, sap);
                    } 
                }));
            }
        }

        if (spellAbility instanceof SpellApiBased && hostCard.isPermanent()) {
            String desc = mapParams.containsKey("SpellDescription") ? mapParams.get("SpellDescription")
                    : spellAbility.getHostCard().getName();
            spellAbility.setDescription(desc);
        } else if (mapParams.containsKey("SpellDescription")) {
            final StringBuilder sb = new StringBuilder();

            if (type != AbilityRecordType.SubAbility) { // SubAbilities don't have Costs or Cost
                              // descriptors
                sb.append(spellAbility.getCostDescription());
            }

            sb.append(mapParams.get("SpellDescription"));

            spellAbility.setDescription(sb.toString());
        } else if (api == ApiType.Charm) {
            spellAbility.setDescription(CharmEffect.makeSpellDescription(spellAbility));
        } else {
            spellAbility.setDescription("");
        }

        initializeParams(spellAbility, mapParams);
        makeRestrictions(spellAbility, mapParams);
        makeConditions(spellAbility, mapParams);

        return spellAbility;
    }

    private static final TargetRestrictions readTarget(Map<String, String> mapParams) {
        final String min = mapParams.containsKey("TargetMin") ? mapParams.get("TargetMin") : "1";
        final String max = mapParams.containsKey("TargetMax") ? mapParams.get("TargetMax") : "1";


        // TgtPrompt now optional
        final String prompt = mapParams.containsKey("TgtPrompt") ? mapParams.get("TgtPrompt") : "Select target " + mapParams.get("ValidTgts");

        TargetRestrictions abTgt = new TargetRestrictions(prompt, mapParams.get("ValidTgts").split(","), min, max);

        if (mapParams.containsKey("TgtZone")) { // if Targeting
                                                     // something
            // not in play, this Key
            // should be set
            abTgt.setZone(ZoneType.listValueOf(mapParams.get("TgtZone")));
        }

        if (mapParams.containsKey("MaxTotalTargetCMC")) {
            // only target cards up to a certain total max CMC
            abTgt.setMaxTotalCMC(mapParams.get("MaxTotalTargetCMC"));
        }

        // TargetValidTargeting most for Counter: e.g. target spell that
        // targets X.
        if (mapParams.containsKey("TargetValidTargeting")) {
            abTgt.setSAValidTargeting(mapParams.get("TargetValidTargeting"));
        }

        if (mapParams.containsKey("TargetsSingleTarget")) {
            abTgt.setSingleTarget(true);
        }
        if (mapParams.containsKey("TargetUnique")) {
            abTgt.setUniqueTargets(true);
        }
        if (mapParams.containsKey("TargetsFromSingleZone")) {
            abTgt.setSingleZone(true);
        }
        if (mapParams.containsKey("TargetsWithoutSameCreatureType")) {
            abTgt.setWithoutSameCreatureType(true);
        }
        if (mapParams.containsKey("TargetsWithSameCreatureType")) {
            abTgt.setWithSameCreatureType(true);
        }
        if (mapParams.containsKey("TargetsWithSameController")) {
            abTgt.setSameController(true);
        }
        if (mapParams.containsKey("TargetsWithDifferentControllers")) {
            abTgt.setDifferentControllers(true);
        }
        if (mapParams.containsKey("DividedAsYouChoose")) {
            abTgt.calculateStillToDivide(mapParams.get("DividedAsYouChoose"), null, null);
            abTgt.setDividedAsYouChoose(true);
        }
        if (mapParams.containsKey("TargetsAtRandom")) {
            abTgt.setRandomTarget(true);
        }
        if (mapParams.containsKey("TargetingPlayer")) {
            abTgt.setMandatory(true);
        }
        return abTgt;
    }

    /**
     * <p>
     * initializeParams.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mapParams
     */
    private static final void initializeParams(final SpellAbility sa, Map<String, String> mapParams) {

        if (mapParams.containsKey("NonBasicSpell")) {
            sa.setBasicSpell(false);
        }
    }

    /**
     * <p>
     * makeRestrictions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mapParams
     */
    private static final void makeRestrictions(final SpellAbility sa, Map<String, String> mapParams) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityRestriction restrict = sa.getRestrictions();
        restrict.setRestrictions(mapParams);
    }

    /**
     * <p>
     * makeConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mapParams
     */
    private static final void makeConditions(final SpellAbility sa, Map<String, String> mapParams) {
        // SpellAbilityRestrictions should be added in here
        final SpellAbilityCondition condition = sa.getConditions();
        condition.setConditions(mapParams);
    }

    // Easy creation of SubAbilities
    /**
     * <p>
     * getSubAbility.
     * </p>
     * @param sSub
     * 
     * @return a {@link forge.game.spellability.AbilitySub} object.
     */
    private static final AbilitySub getSubAbility(CardState state, String sSub, final SpellAbility parent) {

        if (state.hasSVar(sSub)) {
            return (AbilitySub) AbilityFactory.getAbility(state, sSub, parent);
        }
        System.out.println("SubAbility '"+ sSub +"' not found for: " + state.getName());

        return null;
    }

    public static final Map<String, String> getMapParams(final String abString) {
        return FileSection.parseToMap(abString, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
    }

    public static final void adjustChangeZoneTarget(final Map<String, String> params, final SpellAbility sa) {
        if (params.containsKey("Origin")) {
            List<ZoneType> origin = ZoneType.listValueOf(params.get("Origin"));

            final TargetRestrictions tgt = sa.getTargetRestrictions();
        
            // Don't set the zone if it targets a player
            if ((tgt != null) && !tgt.canTgtPlayer()) {
                sa.getTargetRestrictions().setZone(origin);
            }
        }
    
    }

    public static final SpellAbility buildFusedAbility(final Card card) {
        if(!card.isSplitCard()) 
            throw new IllegalStateException("Fuse ability may be built only on split cards");
        
        CardState leftState = card.getState(CardStateName.LeftSplit);
        SpellAbility leftAbility = leftState.getFirstAbility();
        Map<String, String> leftMap = Maps.newHashMap(leftAbility.getMapParams());
        AbilityRecordType leftType = AbilityRecordType.getRecordType(leftMap);
        ApiType leftApi = leftType.getApiTypeOf(leftMap);
        leftMap.put("StackDecription", leftMap.get("SpellDescription"));
        leftMap.put("SpellDescription", "Fuse (you may cast both halves of this card from your hand).");
        leftMap.put("ActivationZone", "Hand");

        CardState rightState = card.getState(CardStateName.RightSplit);
        SpellAbility rightAbility = rightState.getFirstAbility();
        Map<String, String> rightMap = Maps.newHashMap(rightAbility.getMapParams());

        AbilityRecordType rightType = AbilityRecordType.getRecordType(leftMap);
        ApiType rightApi = leftType.getApiTypeOf(rightMap);
        rightMap.put("StackDecription", rightMap.get("SpellDescription"));
        rightMap.put("SpellDescription", "");

        Cost totalCost = parseAbilityCost(leftState, leftMap, leftType);
        totalCost.add(parseAbilityCost(rightState, rightMap, rightType));

        final SpellAbility left = getAbility(leftType, leftApi, leftMap, totalCost, leftState, null);
        final AbilitySub right = (AbilitySub) getAbility(AbilityRecordType.SubAbility, rightApi, rightMap, null, rightState, left);
        left.appendSubAbility(right);
        return left;
    }
} // end class AbilityFactory
