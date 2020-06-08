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
package forge.game.replacement;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardTraitChanges;
import forge.game.card.CardUtil;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordsChange;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import forge.util.TextUtil;
import forge.util.Visitor;
import forge.util.Localizer;
import forge.util.CardTranslation;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.*;

public class ReplacementHandler {
    private final Game game;

    private Set<ReplacementEffect> hasRun = Sets.newHashSet();
    /**
     * ReplacementHandler.
     * @param gameState
     */
    public ReplacementHandler(Game gameState) {
        game = gameState;
    }

    //private final List<ReplacementEffect> tmpEffects = new ArrayList<ReplacementEffect>();

    public List<ReplacementEffect> getReplacementList(final ReplacementType event, final Map<AbilityKey, Object> runParams, final ReplacementLayer layer) {

        final CardCollection preList = new CardCollection();
        boolean checkAgain = false;
        Card affectedLKI = null;
        Card affectedCard = null;

        if (ReplacementType.Moved.equals(event) && ZoneType.Battlefield.equals(runParams.get(AbilityKey.Destination))) {
            // if it was caused by an replacement effect, use the already calculated RE list
            // otherwise the RIOT card would cause a StackError
            SpellAbility cause = (SpellAbility) runParams.get(AbilityKey.Cause);
            if (cause != null && cause.isReplacementAbility()) {
                final ReplacementEffect re = cause.getReplacementEffect();
                // only return for same layer
                if (ReplacementType.Moved.equals(re.getMode()) && layer.equals(re.getLayer())) {
                    if (!re.getOtherChoices().isEmpty())
                        return re.getOtherChoices();
                }
            }

            // Rule 614.12 Enter the Battlefield Replacement Effects look at what the card would be on the battlefield
            affectedCard = (Card) runParams.get(AbilityKey.Affected);
            affectedLKI = CardUtil.getLKICopy(affectedCard);
            affectedLKI.setLastKnownZone(affectedCard.getController().getZone(ZoneType.Battlefield));

            // need to apply Counters to check its future state on the battlefield
            affectedLKI.putEtbCounters(null);
            preList.add(affectedLKI);
            game.getAction().checkStaticAbilities(false, Sets.newHashSet(affectedLKI), preList);
            checkAgain = true;

            // need to check if Intrinsic has run
            for (ReplacementEffect re : affectedLKI.getReplacementEffects()) {
                if (re.isIntrinsic() && this.hasRun.contains(re)) {
                    re.setHasRun(true);
                }
            }

            // need to check non Intrinsic
            for (Map.Entry<Long, CardTraitChanges> e : affectedLKI.getChangedCardTraits().entrySet()) {
                boolean hasRunRE = false;
                String skey = String.valueOf(e.getKey());

                for (ReplacementEffect re : this.hasRun) {
                    if (!re.isIntrinsic() && skey.equals(re.getSVar("_ReplacedTimestamp"))) {
                        hasRunRE = true;
                        break;
                    }
                }

                for (ReplacementEffect re : e.getValue().getReplacements()) {
                    re.setSVar("_ReplacedTimestamp", skey);
                    if (hasRunRE) {
                        re.setHasRun(true);
                    }
                }
            }
            for (Map.Entry<Long, KeywordsChange> e : affectedLKI.getChangedCardKeywords().entrySet()) {
                boolean hasRunRE = false;
                String skey = String.valueOf(e.getKey());

                for (ReplacementEffect re : this.hasRun) {
                    if (!re.isIntrinsic() && skey.equals(re.getSVar("_ReplacedTimestamp"))) {
                        hasRunRE = true;
                        break;
                    }
                }

                for (KeywordInterface k : e.getValue().getKeywords()) {
                    for (ReplacementEffect re : k.getReplacements()) {
                        re.setSVar("_ReplacedTimestamp", skey);
                        if (hasRunRE) {
                            re.setHasRun(true);
                        }
                    }
                }
            }

            runParams.put(AbilityKey.Affected, affectedLKI);
        }

        final List<ReplacementEffect> possibleReplacers = Lists.newArrayList();
        // Round up Non-static replacement effects ("Until EOT," or
        // "The next time you would..." etc)
        /*for (final ReplacementEffect replacementEffect : this.tmpEffects) {
            if (!replacementEffect.hasRun() && replacementEffect.canReplace(runParams) && replacementEffect.getLayer() == layer) {
                possibleReplacers.add(replacementEffect);
            }
        }*/

        // Round up Static replacement effects
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(Card crd) {
                final Card c = preList.get(crd);

                for (final ReplacementEffect replacementEffect : c.getReplacementEffects()) {

                    // Use "CheckLKIZone" parameter to test for effects that care abut where the card was last (e.g. Kalitas, Traitor of Ghet
                    // getting hit by mass removal should still produce tokens).
                    Zone cardZone = "True".equals(replacementEffect.getParam("CheckSelfLKIZone")) ? game.getChangeZoneLKIInfo(c).getLastKnownZone() : game.getZoneOf(c);

                    // Replacement effects that are tied to keywords (e.g. damage prevention effects - if the keyword is removed, the replacement
                    // effect should be inactive)
                    if (replacementEffect.hasParam("TiedToKeyword")) {
                        String kw = replacementEffect.getParam("TiedToKeyword");
                        if (!c.hasKeyword(kw)) {
                            continue;
                        }
                    }

                    if (!replacementEffect.hasRun()
                            && (layer == null || replacementEffect.getLayer() == layer)
                            && event.equals(replacementEffect.getMode())
                            && replacementEffect.requirementsCheck(game)
                            && replacementEffect.canReplace(runParams)
                            && !possibleReplacers.contains(replacementEffect)
                            && replacementEffect.zonesCheck(cardZone)) {
                        possibleReplacers.add(replacementEffect);
                    }
                }
                return true;
            }

        });

        if (checkAgain) {
            if (affectedLKI != null && affectedCard != null) {
                // need to set the Host Card there so it is not connected to LKI anymore?
                // need to be done after canReplace check
                for (final ReplacementEffect re : affectedLKI.getReplacementEffects()) {
                    re.setHostCard(affectedCard);
                }
                runParams.put(AbilityKey.Affected, affectedCard);
            }
            game.getAction().checkStaticAbilities(false);
        }

        return possibleReplacers;
    }

    /**
     *
     * Runs any applicable replacement effects.
     *
     * @param runParams
     *            the run params,same as for triggers.
     * @return ReplacementResult, an enum that represents what happened to the replacement effect.
     */
    public ReplacementResult run(ReplacementType event, final Map<AbilityKey, Object> runParams) {
        final Object affected = runParams.get(AbilityKey.Affected);
        Player decider = null;

        // Figure out who decides which of multiple replacements to apply
        // as well as whether or not to apply optional replacements.
        if (affected instanceof Player) {
            decider = (Player) affected;
        } else {
            decider = ((Card) affected).getController();
        }

        // try out all layer
        for (ReplacementLayer layer : ReplacementLayer.values()) {
            ReplacementResult res = run(event, runParams, layer, decider);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
        }

        return ReplacementResult.NotReplaced;

    }

    private ReplacementResult run(final ReplacementType event, final Map<AbilityKey, Object> runParams, final ReplacementLayer layer, final Player decider) {
        final List<ReplacementEffect> possibleReplacers = getReplacementList(event, runParams, layer);

        if (possibleReplacers.isEmpty()) {
            return ReplacementResult.NotReplaced;
        }

        ReplacementEffect chosenRE = decider.getController().chooseSingleReplacementEffect(Localizer.getInstance().getMessage("lblChooseFirstApplyReplacementEffect"), possibleReplacers);

        possibleReplacers.remove(chosenRE);

        chosenRE.setHasRun(true);
        hasRun.add(chosenRE);
        chosenRE.setOtherChoices(possibleReplacers);
        ReplacementResult res = executeReplacement(runParams, chosenRE, decider, game);
        if (res == ReplacementResult.NotReplaced) {
            if (!possibleReplacers.isEmpty()) {
                res = run(event, runParams);
            }
            chosenRE.setHasRun(false);
            hasRun.remove(chosenRE);
            chosenRE.setOtherChoices(null);
            return res;
        }
        chosenRE.setHasRun(false);
        hasRun.remove(chosenRE);
        chosenRE.setOtherChoices(null);
        String message = chosenRE.getDescription();
        if ( !StringUtils.isEmpty(message))
            if (chosenRE.getHostCard() != null) {
                message = TextUtil.fastReplace(message, "CARDNAME", chosenRE.getHostCard().getName());
            }
            game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
        return res;
    }

    /**
     *
     * Runs a single replacement effect.
     *
     * @param replacementEffect
     *            the replacement effect to run
     */
    private ReplacementResult executeReplacement(final Map<AbilityKey, Object> runParams,
        final ReplacementEffect replacementEffect, final Player decider, final Game game) {
        final Map<String, String> mapParams = replacementEffect.getMapParams();

        SpellAbility effectSA = null;

        Card host = replacementEffect.getHostCard();
        // AlternateState for OriginsPlaneswalker
        // FaceDown for cards like Necropotence
        if (host.hasAlternateState() || host.isFaceDown()) {
            host = game.getCardState(host);
        }

        if (mapParams.containsKey("ReplaceWith")) {
            final String effectSVar = mapParams.get("ReplaceWith");
            final String effectAbString = host.getSVar(effectSVar);
            // TODO: the source of replacement effect should be the source of the original effect
            effectSA = AbilityFactory.getAbility(effectAbString, host);
            //effectSA.setTrigger(true);

            SpellAbility tailend = effectSA;
            do {
                replacementEffect.setReplacingObjects(runParams, tailend);
                //set original Params to update them later
                tailend.setReplacingObject(AbilityKey.OriginalParams, runParams);
                tailend = tailend.getSubAbility();
            } while(tailend != null);

        }
        else if (replacementEffect.getOverridingAbility() != null) {
            effectSA = replacementEffect.getOverridingAbility();
            SpellAbility tailend = effectSA;
            do {
                replacementEffect.setReplacingObjects(runParams, tailend);
                //set original Params to update them later
                tailend.setReplacingObject(AbilityKey.OriginalParams, runParams);
                tailend = tailend.getSubAbility();
            } while(tailend != null);
        }

        if (effectSA != null) {
            effectSA.setLastStateBattlefield(game.getLastStateBattlefield());
            effectSA.setLastStateGraveyard(game.getLastStateGraveyard());
            if (replacementEffect.isIntrinsic()) {
                effectSA.setIntrinsic(true);
                effectSA.changeText();
            }
            effectSA.setReplacementEffect(replacementEffect);
        }

        // Decider gets to choose whether or not to apply the replacement.
        if (replacementEffect.hasParam("Optional")) {
            Player optDecider = decider;
            if (mapParams.containsKey("OptionalDecider") && (effectSA != null)) {
                effectSA.setActivatingPlayer(host.getController());
                optDecider = AbilityUtils.getDefinedPlayers(host,
                        mapParams.get("OptionalDecider"), effectSA).get(0);
            }

            Card cardForUi = host.getCardForUi();
            String effectDesc = TextUtil.fastReplace(replacementEffect.getDescription(), "CARDNAME", CardTranslation.getTranslatedName(cardForUi.getName()));
            final String question = replacementEffect instanceof ReplaceDiscard
                ? Localizer.getInstance().getMessage("lblApplyCardReplacementEffectToCardConfirm", CardTranslation.getTranslatedName(cardForUi.getName()), runParams.get(AbilityKey.Card).toString(), effectDesc)
                : Localizer.getInstance().getMessage("lblApplyReplacementEffectOfCardConfirm", CardTranslation.getTranslatedName(cardForUi.getName()), effectDesc);
            boolean confirmed = optDecider.getController().confirmReplacementEffect(replacementEffect, effectSA, question);
            if (!confirmed) {
                return ReplacementResult.NotReplaced;
            }
        }

        if (mapParams.containsKey("Prevent")) {
            if (mapParams.get("Prevent").equals("True")) {
                return ReplacementResult.Prevented; // Nothing should replace the event.
            }
        }

        Player player = host.getController();

        if (mapParams.containsKey("ManaReplacement")) {
            final SpellAbility manaAb = (SpellAbility) runParams.get(AbilityKey.AbilityMana);
            final Player player1 = (Player) runParams.get(AbilityKey.Player);
            final String rep = (String) runParams.get(AbilityKey.Mana);
            // Replaced mana type
            final Card repHost = host;
            String repType = repHost.getSVar(mapParams.get("ManaReplacement"));
            if (repType.contains("Chosen") && repHost.hasChosenColor()) {
                repType = TextUtil.fastReplace(repType, "Chosen", MagicColor.toShortString(repHost.getChosenColor()));
            }
            manaAb.getManaPart().setManaReplaceType(repType);
            manaAb.getManaPart().produceMana(rep, player1, manaAb);
        } else {
            player.getController().playSpellAbilityNoStack(effectSA, true);
            // if the spellability is a replace effect then its some new logic
            // if ReplacementResult is set in run params use that instead
            if (runParams.containsKey(AbilityKey.ReplacementResult)) {
                return (ReplacementResult) runParams.get(AbilityKey.ReplacementResult);
            }
        }

        return ReplacementResult.Replaced;
    }

    /**
     *
     * Creates an instance of the proper replacement effect object based on raw
     * script.
     *
     * @param repParse
     *            A raw line of script
     * @param host
     *            The cards that hosts the replacement effect.
     * @return A finished instance
     */
    public static ReplacementEffect parseReplacement(final String repParse, final Card host, final boolean intrinsic) {
        return ReplacementHandler.parseReplacement(parseParams(repParse), host, intrinsic);
    }

    public static Map<String, String> parseParams(final String repParse) {
        return FileSection.parseToMap(repParse, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
    }

    /**
     *
     * Creates an instance of the proper replacement effect object based on a
     * parsed script.
     *
     * @param mapParams
     *            The parsed script
     * @param host
     *            The card that hosts the replacement effect
     * @return The finished instance
     */
    private static ReplacementEffect parseReplacement(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        final ReplacementType rt = ReplacementType.smartValueOf(mapParams.get("Event"));
        ReplacementEffect ret = rt.createReplacement(mapParams, host, intrinsic);

        String activeZones = mapParams.get("ActiveZones");
        if (null != activeZones) {
            ret.setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(activeZones)));
        }

        return ret;
    }
}
