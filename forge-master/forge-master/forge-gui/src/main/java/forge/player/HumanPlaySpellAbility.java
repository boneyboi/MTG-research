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
package forge.player;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardPlayOption;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPayment;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.*;
import forge.game.zone.Zone;
import forge.util.collect.FCollection;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 *
 * @author Forge
 * @version $Id: HumanPlaySpellAbility.java 24317 2014-01-17 08:32:39Z Max mtg $
 */
public class HumanPlaySpellAbility {
    private final PlayerControllerHuman controller;
    private SpellAbility ability;

    public HumanPlaySpellAbility(final PlayerControllerHuman controller0, final SpellAbility ability0) {
        controller = controller0;
        ability = ability0;
    }

    public final boolean playAbility(final boolean mayChooseTargets, final boolean isFree, final boolean skipStack) {
        final Player human = ability.getActivatingPlayer();
        final Game game = ability.getActivatingPlayer().getGame();

        // used to rollback
        Zone fromZone = null;
        CardStateName fromState = null;
        int zonePosition = 0;
        final ManaPool manapool = human.getManaPool();

        final Card c = ability.getHostCard();
        final CardPlayOption option = c.mayPlay(ability.getMayPlay());

        boolean manaTypeConversion = false;
        boolean manaColorConversion = false;

        if (ability.isSpell()) {
            if (c.hasKeyword("May spend mana as though it were mana of any type to cast CARDNAME")
                    || (option != null && option.isIgnoreManaCostType())) {
                manaTypeConversion = true;
            } else  if (c.hasKeyword("May spend mana as though it were mana of any color to cast CARDNAME")
                    || (option != null && option.isIgnoreManaCostColor())) {
                manaColorConversion = true;
            }
        }
        
        final boolean playerManaConversion = human.hasManaConversion()
                && human.getController().confirmAction(ability, null, "Do you want to spend mana as though it were mana of any color to pay the cost?");

        boolean keywordColor = false;
        // freeze Stack. No abilities should go onto the stack while I'm filling requirements.
        game.getStack().freezeStack();

        if (ability instanceof Spell && !c.isCopiedSpell()) {
            fromZone = game.getZoneOf(c);
            fromState = c.getCurrentStateName();
            if (fromZone != null) {
                zonePosition = fromZone.getCards().indexOf(c);
            }
            // This is should happen earlier, before the Modal spell is chosen
            // Turn face-down card face up (except case of morph spell)
            if (ability.isSpell() && !ability.isCastFaceDown() && fromState == CardStateName.FaceDown) {
                c.turnFaceUp();
            }
            ability.setHostCard(game.getAction().moveToStack(c, ability));
        }

        if (!ability.isCopied()) {
            ability.resetPaidHash();
        }

        ability = GameActionUtil.addExtraKeywordCost(ability);

        Cost abCost = ability.getPayCosts();
        CostPayment payment = new CostPayment(abCost, ability);

        // TODO Apply this to the SAStackInstance instead of the Player
        if (manaTypeConversion) {
            AbilityUtils.applyManaColorConversion(payment, MagicColor.Constant.ANY_TYPE_CONVERSION);
        } else if (manaColorConversion) {
            AbilityUtils.applyManaColorConversion(payment, MagicColor.Constant.ANY_COLOR_CONVERSION);
        }

        if (playerManaConversion) {
            AbilityUtils.applyManaColorConversion(manapool, MagicColor.Constant.ANY_COLOR_CONVERSION);
            human.incNumManaConversion();
        }
        
        if (ability.isAbility() && ability instanceof AbilityActivated) {
            final Map<String, String> params = Maps.newHashMap();
            params.put("ManaColorConversion", "Additive");

            for (KeywordInterface inst : c.getKeywords()) {
                String keyword = inst.getOriginal();
                if (keyword.startsWith("ManaConvert")) {
                    final String[] k = keyword.split(":");
                    params.put(k[1] + "Conversion", k[2]);
                    keywordColor = true;
                }
            }

            if (keywordColor) {
                AbilityUtils.applyManaColorConversion(payment, params);
            }
        }

        // This line makes use of short-circuit evaluation of boolean values, that is each subsequent argument
        // is only executed or evaluated if the first argument does not suffice to determine the value of the expression
        final boolean prerequisitesMet = announceValuesLikeX()
                && announceType()
                && (!mayChooseTargets || setupTargets()) // if you can choose targets, then do choose them.
                && (isFree || payment.payCost(new HumanCostDecision(controller, human, ability, ability.getHostCard())));

        if (!prerequisitesMet) {
            if (!ability.isTrigger()) {
                rollbackAbility(fromZone, zonePosition, payment);
                if (ability.getHostCard().isMadness()) {
                    // if a player failed to play madness cost, move the card to graveyard
                    Card newCard = game.getAction().moveToGraveyard(c, null);
                    newCard.setMadnessWithoutCast(true);
                    newCard.setMadness(false);
                } else if (ability.getHostCard().isBestowed()) {
                    ability.getHostCard().unanimateBestow();
                }
            }

            if (manaTypeConversion || manaColorConversion || keywordColor) {
                manapool.restoreColorReplacements();
            }
            if (playerManaConversion) {
                manapool.restoreColorReplacements();
                human.decNumManaConversion();
            }
            return false;
        }

        if (isFree || payment.isFullyPaid()) {
            //track when planeswalker ultimates are activated
            ability.getActivatingPlayer().getAchievementTracker().onSpellAbilityPlayed(ability);

            if (skipStack) {
                AbilityUtils.resolve(ability);
            } else {
                enusureAbilityHasDescription(ability);
                game.getStack().addAndUnfreeze(ability);
            }

            // no worries here. The same thread must resolve, and by this moment ability will have been resolved already
            // Triggers haven't resolved yet ??
            if (mayChooseTargets) {
                clearTargets(ability);
            }
            if (manaTypeConversion || manaColorConversion || keywordColor) {
                manapool.restoreColorReplacements();
            }
        }
        return true;
    }

    private final boolean setupTargets() {
        // Skip to paying if parent ability doesn't target and has no subAbilities.
        // (or trigger case where its already targeted)
        SpellAbility currentAbility = ability;
        final Card source = ability.getHostCard();
        do {
            final TargetRestrictions tgt = currentAbility.getTargetRestrictions();
            if (tgt != null && tgt.doesTarget()) {
                clearTargets(currentAbility);
                Player targetingPlayer;
                if (currentAbility.hasParam("TargetingPlayer")) {
                    final FCollection<Player> candidates = AbilityUtils.getDefinedPlayers(source, currentAbility.getParam("TargetingPlayer"), currentAbility);
                    // activator chooses targeting player
                    targetingPlayer = ability.getActivatingPlayer().getController().chooseSingleEntityForEffect(
                            candidates, currentAbility, "Choose the targeting player");
                } else {
                    targetingPlayer = ability.getActivatingPlayer();
                }
                currentAbility.setTargetingPlayer(targetingPlayer);
                if (!targetingPlayer.getController().chooseTargetsFor(currentAbility)) {
                    return false;
                }
            }
            final SpellAbility subAbility = currentAbility.getSubAbility();
            if (subAbility != null) {
                // This is necessary for "TargetsWithDefinedController$ ParentTarget"
                ((AbilitySub) subAbility).setParent(currentAbility);
            }
            currentAbility = subAbility;
        } while (currentAbility != null);
        return true;
    }

    public final void clearTargets(final SpellAbility ability) {
        final TargetRestrictions tg = ability.getTargetRestrictions();
        if (tg != null) {
            ability.resetTargets();
            tg.calculateStillToDivide(ability.getParam("DividedAsYouChoose"), ability.getHostCard(), ability);
        }
    }

    private void rollbackAbility(final Zone fromZone, final int zonePosition, CostPayment payment) {
        // cancel ability during target choosing
        final Game game = ability.getActivatingPlayer().getGame();

        if (fromZone != null) { // and not a copy
            ability.getHostCard().setCastSA(null);
            ability.getHostCard().setCastFrom(null);
            // add back to where it came from
            game.getAction().moveTo(fromZone, ability.getHostCard(), zonePosition >= 0 ? Integer.valueOf(zonePosition) : null, null);
        }

        clearTargets(ability);

        ability.resetOnceResolved();
        payment.refundPayment();
        game.getStack().clearFrozen();
        game.getTriggerHandler().clearWaitingTriggers();
    }

    private boolean announceValuesLikeX() {
        if (ability.isCopied()) { return true; } //don't re-announce for spell copies

        boolean needX = true;
        final boolean allowZero = !ability.hasParam("XCantBe0");
        final Cost cost = ability.getPayCosts();
        final CostPartMana manaCost = cost.getCostMana();
        final PlayerController controller = ability.getActivatingPlayer().getController();
        final Card card = ability.getHostCard();

        // Announcing Requirements like Choosing X or Multikicker
        // SA Params as comma delimited list
        final String announce = ability.getParam("Announce");
        if (announce != null) {
            for (final String aVar : announce.split(",")) {
                final String varName = aVar.trim();

                final boolean isX = "X".equalsIgnoreCase(varName);
                if (isX) { needX = false; }

                final Integer value = controller.announceRequirements(ability, varName, allowZero && (!isX || manaCost == null || manaCost.canXbe0()));
                if (value == null) {
                    return false;
                }

                ability.setSVar(varName, value.toString());
                if ("Multikicker".equals(varName)) {
                    card.setKickerMagnitude(value);
                } else if ("Pseudo-multikicker".equals(varName)) {
                    card.setPseudoMultiKickerMagnitude(value);
                }
                else {
                    card.setSVar(varName, value.toString());
                }
            }
        }

        if (needX && manaCost != null) {
            boolean xInCost = manaCost.getAmountOfX() > 0;
            if (!xInCost) {
                for (final CostPart part : cost.getCostParts()) {
                    if (part.getAmount().equals("X")) {
                        xInCost = true;
                        break;
                    }
                }
            }
            if (xInCost) {
                final String sVar = ability.getSVar("X"); //only prompt for new X value if card doesn't determine it another way
                if ("Count$xPaid".equals(sVar) || sVar.isEmpty()) {
                    final Integer value = controller.announceRequirements(ability, "X", allowZero && manaCost.canXbe0());
                    if (value == null) {
                        return false;
                    }
                    ability.setXManaCostPaid(value);
                }
            } else if (manaCost.getMana().isZero() && ability.isSpell()) {
                ability.setXManaCostPaid(0);
            }
        }
        return true;
    }

    // Announcing Requirements like choosing creature type or number
    private boolean announceType() {
        if (ability.isCopied()) { return true; } //don't re-announce for spell copies

        final String announce = ability.getParam("AnnounceType");
        final PlayerController pc = ability.getActivatingPlayer().getController();
        if (announce != null) {
            for (final String aVar : announce.split(",")) {
                final String varName = aVar.trim();
                if ("CreatureType".equals(varName)) {
                    final String choice = pc.chooseSomeType("Creature", ability, CardType.Constant.CREATURE_TYPES, Collections.emptyList());
                    ability.getHostCard().setChosenType(choice);
                }
                if ("ChooseNumber".equals(varName)) {
                    final int min = Integer.parseInt(ability.getParam("Min"));
                    final int max = Integer.parseInt(ability.getParam("Max"));
                    final int i = ability.getActivatingPlayer().getController().chooseNumber(ability,
                            "Choose a number", min, max);
                    ability.getHostCard().setChosenNumber(i);
                }
            }
        }
        return true;
    }

    private static void enusureAbilityHasDescription(final SpellAbility ability) {
        if (!StringUtils.isBlank(ability.getStackDescription())) {
            return;
        }

        // For older abilities that don't setStackDescription set it here
        final StringBuilder sb = new StringBuilder();
        sb.append(ability.getHostCard().getName());
        if (ability.getTargetRestrictions() != null) {
            final Iterable<GameObject> targets = ability.getTargets().getTargets();
            if (!Iterables.isEmpty(targets)) {
                sb.append(" - Targeting ");
                for (final GameObject o : targets) {
                    sb.append(o.toString()).append(" ");
                }
            }
        }

        ability.setStackDescription(sb.toString());
    }
}
