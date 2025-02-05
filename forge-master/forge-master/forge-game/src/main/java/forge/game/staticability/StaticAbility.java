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
package forge.game.staticability;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.card.MagicColor;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameStage;
import forge.game.IIdentifiable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.TextUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Class StaticAbility.
 */
public class StaticAbility extends CardTraitBase implements IIdentifiable, Cloneable, Comparable<StaticAbility> {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    private int id;

    private Set<StaticAbilityLayer> layers;
    private CardCollectionView ignoreEffectCards = new CardCollection();
    private final List<Player> ignoreEffectPlayers = Lists.newArrayList();
    private int mayPlayTurn = 0;

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(StaticAbility.class, getId());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof StaticAbility && this.id == ((StaticAbility) obj).id;
    }

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     *
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    private static Map<String, String> parseParams(final String abString, final Card hostCard) {
        final Map<String, String> mapParameters = Maps.newHashMap();

        if (!(abString.length() > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- abString too short in "
                    + hostCard.getName() + ": [" + abString + "]");
        }

        final String[] a = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++) {
            a[aCnt] = a[aCnt].trim();
        }

        if (!(a.length > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- a[] too short in " + hostCard.getName());
        }

        for (final String element : a) {
            final String[] aa = element.split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                aa[aaCnt] = aa[aaCnt].trim();
            }

            if (aa.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("StaticEffectFactory Parsing Error: Split length of ");
                sb.append(element).append(" in ").append(hostCard.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParameters.put(aa[0], aa[1]);
        }

        return mapParameters;
    }

    /**
     * Gets the {@link Set} of {@link StaticAbilityLayer}s in which this
     * {@link StaticAbility} is to be applied.
     *
     * @return the applicable layers.
     */
    private final Set<StaticAbilityLayer> generateLayer() {
        if (!getParam("Mode").equals("Continuous")) {
            return EnumSet.noneOf(StaticAbilityLayer.class);
        }

        final Set<StaticAbilityLayer> layers = EnumSet.noneOf(StaticAbilityLayer.class);
        if (hasParam("GainControl")) {
            layers.add(StaticAbilityLayer.CONTROL);
        }

        if (hasParam("ChangeColorWordsTo") || hasParam("GainTextOf")) {
            layers.add(StaticAbilityLayer.TEXT);
        }

        if (hasParam("AddType") || hasParam("RemoveType")
                || hasParam("RemoveCardTypes") || hasParam("RemoveSubTypes")
                || hasParam("RemoveSuperTypes") || hasParam("RemoveLandTypes")
                || hasParam("RemoveCreatureTypes") || hasParam("RemoveArtifactTypes")
                || hasParam("RemoveEnchantmentTypes")) {
            layers.add(StaticAbilityLayer.TYPE);
        }

        if (hasParam("AddColor") || hasParam("RemoveColor") || hasParam("SetColor")) {
            layers.add(StaticAbilityLayer.COLOR);
        }

        if (hasParam("RemoveAllAbilities") || hasParam("GainsAbilitiesOf")) {
            layers.add(StaticAbilityLayer.ABILITIES);
        }

        if (hasParam("AddKeyword") || hasParam("AddAbility")
                || hasParam("AddTrigger") || hasParam("RemoveTriggers")
                || hasParam("RemoveKeyword") || hasParam("AddReplacementEffects")
                || hasParam("AddStaticAbility") || hasParam("AddSVar")
                || hasParam("CantHaveKeyword")) {
            layers.add(StaticAbilityLayer.ABILITIES);
        }

        if (hasParam("CharacteristicDefining")) {
            layers.add(StaticAbilityLayer.CHARACTERISTIC);
        }

        if (hasParam("SetPower") || hasParam("SetToughness")) {
            layers.add(StaticAbilityLayer.SETPT);
        }
        if (hasParam("AddPower") || hasParam("AddToughness")) {
            layers.add(StaticAbilityLayer.MODIFYPT);
        }

        if (hasParam("AddHiddenKeyword")
                || hasParam("IgnoreEffectCost") || hasParam("Goad") || hasParam("CanBlockAny") || hasParam("CanBlockAmount")
                || hasParam("AdjustLandPlays") || hasParam("ControlVote") || hasParam("AdditionalVote") || hasParam("AdditionalOptionalVote")) {
            layers.add(StaticAbilityLayer.RULES);
        }

        if (layers.isEmpty()) {
            layers.add(StaticAbilityLayer.RULES);
        }

        return layers;
    }

    private void buildCommonAttributes(Card host) {
        if (hasParam("References")) {
            for (String svar : getParam("References").split(",")) {
                if (host.hasSVar(svar)) {
                    this.setSVar(svar, host.getSVar(svar));
                }
            }
        }
    }

    /**
     * <p>
     * toString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        if (hasParam("Description") && !this.isSuppressed()) {
            String desc = getParam("Description");
            desc = TextUtil.fastReplace(desc, "CARDNAME", this.hostCard.getName());

            return desc;
        } else {
            return "";
        }
    }

    // main constructor
    /**
     * Instantiates a new static ability.
     *
     * @param params
     *            the params
     * @param host
     *            the host
     */
    public StaticAbility(final String params, final Card host) {
        this(parseParams(params, host), host);
    }

    /**
     * Instantiates a new static ability.
     *
     * @param params
     *            the params
     * @param host
     *            the host
     */
    private StaticAbility(final Map<String, String> params, final Card host) {
        this.id = nextId();
        this.originalMapParams.putAll(params);
        this.mapParams.putAll(params);
        this.layers = this.generateLayer();
        this.hostCard = host;
        buildCommonAttributes(host);
    }

    public final CardCollectionView applyContinuousAbilityBefore(final StaticAbilityLayer layer, final CardCollectionView preList) {
        if (!shouldApplyContinuousAbility(layer, false)) {
            return null;
        }
        return StaticAbilityContinuous.applyContinuousAbility(this, layer, preList);
    }

    public final CardCollectionView applyContinuousAbility(final StaticAbilityLayer layer, final CardCollectionView affected) {
        if (!shouldApplyContinuousAbility(layer, true)) {
            return null;
        }
        return StaticAbilityContinuous.applyContinuousAbility(this, affected, layer);
    }

    /**
     * Check whether a continuous ability should be applied.
     *
     * @param layer
     *            the {@link StaticAbilityLayer} under investigation.
     * @param ignoreTempSuppression
     *            whether to ignore temporary suppression of this ability, to be
     *            used when this ability has already begun applying in another
     *            layer and has since been removed from its host card by another
     *            effect (see rule 613.5).
     * @return {@code true} if and only if this is a continuous ability that
     *         affects the specified layer, it's not suppressed, and its
     *         conditions are fulfilled.
     */
    private boolean shouldApplyContinuousAbility(final StaticAbilityLayer layer, final boolean previousRun) {
        return getParam("Mode").equals("Continuous") && layers.contains(layer) && !isSuppressed() && checkConditions() && (previousRun || getHostCard().getStaticAbilities().contains(this));
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     *
     * @param mode
     *            the mode
     * @param source
     *            the source
     * @param target
     *            the target
     * @param in
     *            the in
     * @param isCombat
     *            the b
     * @return the int
     */
    public final int applyAbility(final String mode, final Card source, final GameEntity target, final int in,
            final boolean isCombat, final boolean isTest) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return in;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return in;
        }

        if (mode.equals("PreventDamage")) {
            return StaticAbilityPreventDamage.applyPreventDamageAbility(this, source, target, in, isCombat, isTest);
        }

        return in;
    }

    /**
     * Apply ability if it has the right mode.
     *
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param player
     *            the activator
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final Player player) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkPlayerSpecificConditions(player)) {
            return false;
        }

        if (mode.equals("CantBeCast")) {
            return StaticAbilityCantBeCast.applyCantBeCastAbility(this, card, player);
        }

        if (mode.equals("CantPlayLand")) {
            return StaticAbilityCantBeCast.applyCantPlayLandAbility(this, card, player);
        }

        return false;
    }

    /**
     * Apply ability.
     *
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param spellAbility
     *            the ability
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final SpellAbility spellAbility) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantBeActivated")) {
            return StaticAbilityCantBeCast.applyCantBeActivatedAbility(this, card, spellAbility);
        }

        if (mode.equals("CantTarget")) {
            return StaticAbilityCantTarget.applyCantTargetAbility(this, card, spellAbility);
        }

        return false;
    }

    public final boolean applyAbility(final String mode, final Player player, final SpellAbility spellAbility) {
        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantTarget")) {
            return StaticAbilityCantTarget.applyCantTargetAbility(this, player, spellAbility);
        }

        return false;
    }

    public final boolean applyAbility(String mode, Card card, CounterType type) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantPutCounter")) {
            return StaticAbilityCantPutCounter.applyCantPutCounter(this, card, type);

        }

        return false;
    }

    public final boolean applyAbility(String mode, Player player, CounterType type) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantPutCounter")) {
            return StaticAbilityCantPutCounter.applyCantPutCounter(this, player, type);

        }

        return false;
    }

    /**
     * Apply ability.
     *
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("ETBTapped")) {
            return StaticAbilityETBTapped.applyETBTappedAbility(this, card);
        }

        if (mode.equals("GainAbilitiesOf")) {

        }

        return false;
    }

    public final boolean applyAbility(final String mode, final Card card, final boolean isCombat) {
        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantPreventDamage")) {
            return StaticAbilityCantPreventDamage.applyCantPreventDamage(this, card, isCombat);
        }

        return false;
    }

    /**
     * Apply ability.
     *
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param target
     *            the target
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final GameEntity target) {

        // don't apply the ability if it hasn't got the right mode
        if (!getParam("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantAttack")) {
            return StaticAbilityCantAttackBlock.applyCantAttackAbility(this, card, target);
        } else if (mode.equals("CantBlockBy") && target instanceof Card) {
            return StaticAbilityCantAttackBlock.applyCantBlockByAbility(this, card, (Card)target);
        } else if (mode.equals("CantAttach")) {
            return StaticAbilityCantAttach.applyCantAttachAbility(this, card, target);
        }

        return false;
    }

    public final Cost getAttackCost(final Card attacker, final GameEntity target) {
        if (this.isSuppressed() || !getParam("Mode").equals("CantAttackUnless") || !this.checkConditions()) {
            return null;
        }
        return StaticAbilityCantAttackBlock.getAttackCost(this, attacker, target);
    }

    public final Cost getBlockCost(final Card blocker, final Card attacker) {
        if (this.isSuppressed() || !getParam("Mode").equals("CantBlockUnless") || !this.checkConditions()) {
            return null;
        }
        return StaticAbilityCantAttackBlock.getBlockCost(this, blocker, attacker);
    }

    /**
     * Check conditions for static abilities acting on a specific player. Also
     * automatically check the general conditions.
     *
     * @param player a {@link Player}.
     * @return true, if the static ability is applicable.
     * @see {@link StaticAbility#checkConditions()}
     */
    public final boolean checkPlayerSpecificConditions(final Player player) {
        if (!checkConditions()) {
            return false;
        }

        if (hasParam("PlayerAttackedWithCreatureThisTurn")
                && player.getCreaturesAttackedThisTurn().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Check conditions.
     *
     * @return true, if the static ability is applicable.
     */
    public final boolean checkConditions() {
        final Player controller = getHostCard().getController();
        final Game game = controller.getGame();
        final PhaseHandler ph = game.getPhaseHandler();

        if (getHostCard().isPhasedOut()) {
            return false;
        }

        if (hasParam("EffectZone")) {
            if (!getParam("EffectZone").equals("All")) {
                Zone zone = game.getZoneOf(getHostCard());
                if (zone == null || !ZoneType.listValueOf(getParam("EffectZone")).contains(zone.getZoneType())) {
                    return false;
                }
            }
        } else {
            if (!getHostCard().isInZone(ZoneType.Battlefield)) { // default
                return false;
            }
        }

        String condition = getParam("Condition");
        if (null != condition) {
            if (condition.equals("Threshold") && !controller.hasThreshold()) return false;
            if (condition.equals("Hellbent") && !controller.hasHellbent()) return false;
            if (condition.equals("Metalcraft") && !controller.hasMetalcraft()) return false;
            if (condition.equals("Delirium") && !controller.hasDelirium()) return false;
            if (condition.equals("Ferocious") && !controller.hasFerocious()) return false;
            if (condition.equals("Desert") && !controller.hasDesert()) return false;
            if (condition.equals("Blessing") && !controller.hasBlessing()) return false;

            if (condition.equals("PlayerTurn")) {
                if (!ph.isPlayerTurn(controller)) {
                    return false;
                }
            } else if (condition.equals("NotPlayerTurn")) {
                if (ph.isPlayerTurn(controller)) {
                    return false;
                }
            } else if (condition.equals("PermanentOfEachColor")) {
                if ((controller.getColoredCardsInPlay(MagicColor.Constant.BLACK).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.BLUE).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.GREEN).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.RED).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.WHITE).isEmpty())) {
                    return false;
                }
            } else if (condition.equals("FatefulHour")) {
                if (controller.getLife() > 5) {
                    return false;
                }
            }
        }

        if (hasParam("Phases")) {
            if (!PhaseType.parseRange(getParam("Phases")).contains(ph.getPhase())) {
                return false;
            }
        }

        if (hasParam("PlayerTurn")) {
            List<Player> players = AbilityUtils.getDefinedPlayers(hostCard, getParam("PlayerTurn"), null);
            if (!players.contains(ph.getPlayerTurn())) {
                return false;
            }
        }

        if (hasParam("TopCardOfLibraryIs")) {
            if (controller.getCardsIn(ZoneType.Library).isEmpty()) {
                return false;
            }
            final Card topCard = controller.getCardsIn(ZoneType.Library).get(0);
            if (!topCard.isValid(getParam("TopCardOfLibraryIs").split(","), controller, this.hostCard, null)) {
                return false;
            }
        }

        if (hasParam("IsPresent")) {
            final ZoneType zone = hasParam("PresentZone") ? ZoneType.valueOf(getParam("PresentZone")) : ZoneType.Battlefield;
            final String compare = hasParam("PresentCompare") ? getParam("PresentCompare") : "GE1";
            CardCollectionView list = game.getCardsIn(zone);
            final String present = getParam("IsPresent");

            list = CardLists.getValidCards(list, present.split(","), controller, hostCard, null);

            int right = 1;
            final String rightString = compare.substring(2);
            right = AbilityUtils.calculateAmount(hostCard, rightString, this);
            final int left = list.size();

            if (!Expressions.compare(left, compare, right)) {
                return false;
            }
        }

        if (hasParam("GameStage")) {
            String[] stageDefs = TextUtil.split(getParam("GameStage"), ',');
            boolean isRelevantStage = false;
            for (String stage : stageDefs) {
                isRelevantStage |= (game.getAge() == GameStage.valueOf(stage));
            }
            return isRelevantStage;
        }

        if (hasParam("Presence")) {
            if (hostCard.getCastFrom() == null || hostCard.getCastSA() == null)
                return false;

            final String type = getParam("Presence");

            int revealed = AbilityUtils.calculateAmount(hostCard, "Revealed$Valid " + type, hostCard.getCastSA());
            int ctrl = AbilityUtils.calculateAmount(hostCard, "Count$LastStateBattlefield " + type + ".YouCtrl", hostCard.getCastSA());

            if (revealed + ctrl == 0) {
                return false;
            }
        }

        if (hasParam("CheckSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckSVar"), this);
            String comparator = "GE1";
            if (hasParam("SVarCompare")) {
                comparator = getParam("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (hasParam("CheckSecondSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckSecondSVar"), this);
            String comparator = "GE1";
            if (hasParam("SecondSVarCompare")) {
                comparator = getParam("SecondSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (hasParam("CheckThirdSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckThirdSVar"), this);
            String comparator = "GE1";
            if (hasParam("ThirdSVarCompare")) {
                comparator = getParam("ThirdSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (hasParam("CheckFourthSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckFourthSVar"), this);
            String comparator = "GE1";
            if (hasParam("FourthSVarCompare")) {
                comparator = getParam("FourthSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return the ignoreEffectCards
     */
    public CardCollectionView getIgnoreEffectCards() {
        return ignoreEffectCards;
    }

    /**
     * @param cards the ignoreEffectCards to set
     */
    public void setIgnoreEffectCards(final CardCollectionView cards) {
        ignoreEffectCards = cards;
    }

    /**
     * @return the ignoreEffectPlayers
     */
    public List<Player> getIgnoreEffectPlayers() {
        return ignoreEffectPlayers;
    }

    /**
     * @param p the ignoreEffectPlayers to add
     */
    public void addIgnoreEffectPlayers(final Player p) {
        ignoreEffectPlayers.add(p);
    }

    public void clearIgnoreEffects() {
        ignoreEffectPlayers.clear();
        ignoreEffectCards = new CardCollection();
    }

    /**
     * @return the layer
     */
    public Set<StaticAbilityLayer> getLayers() {
        return layers;
    }


    public int getMayPlayTurn() {
        return mayPlayTurn;
    }

    public void incMayPlayTurn() {
        this.mayPlayTurn++;
    }

    public void resetMayPlayTurn() {
        this.mayPlayTurn = 0;
    }

    @Override
    public int compareTo(StaticAbility arg0) {
        return ComparisonChain.start()
        .compare(getHostCard(),arg0.getHostCard())
        .compare(getId(), arg0.getId())
        .result();
    }

    public StaticAbility copy(Card host, final boolean lki) {
        StaticAbility clone = null;
        try {
            clone = (StaticAbility) clone();
            clone.id = lki ? id : nextId();

            copyHelper(clone, host);

            clone.layers = this.generateLayer();

            clone.buildCommonAttributes(host);
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }
} // end class StaticAbility
