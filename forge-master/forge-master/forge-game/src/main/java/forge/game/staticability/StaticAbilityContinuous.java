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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.GameCommand;
import forge.card.ColorSet;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.StaticEffect;
import forge.game.StaticEffects;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * The Class StaticAbility_Continuous.
 */
public final class StaticAbilityContinuous {

    // Private constructor to prevent instantiation
    private StaticAbilityContinuous() {
    }

    /**
     * Apply the effects of a static ability that apply in a particular layer.
     * The cards to which the effects are applied are dynamically determined.
     *
     * @param stAb
     *            a {@link StaticAbility}.
     * @param layer
     *            the {@link StaticAbilityLayer} of effects to apply.
     * @return a {@link CardCollectionView} of cards that have been affected.
     * @see #getAffectedCards(StaticAbility)
     * @see #applyContinuousAbility(StaticAbility, CardCollectionView,
     *      StaticAbilityLayer)
     */
	public static CardCollectionView applyContinuousAbility(final StaticAbility stAb, final StaticAbilityLayer layer, final CardCollectionView preList) {
        final CardCollectionView affectedCards = getAffectedCards(stAb, preList);
        return applyContinuousAbility(stAb, affectedCards, layer);
    }

    /**
     * Apply the effects of a static ability that apply in a particular layer to
     * a predefined set of cards.
     *
     * @param stAb
     *            a {@link StaticAbility}.
     * @param affectedCards
     *            a {@link CardCollectionView} of cards that are to be affected.
     * @param layer
     *            the {@link StaticAbilityLayer} of effects to apply.
     * @return a {@link CardCollectionView} of cards that have been affected,
     *         identical to {@code affectedCards}.
     */
    public static CardCollectionView applyContinuousAbility(final StaticAbility stAb, final CardCollectionView affectedCards, final StaticAbilityLayer layer) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        final List<Player> affectedPlayers = StaticAbilityContinuous.getAffectedPlayers(stAb);
        final Game game = hostCard.getGame();

        final StaticEffect se = game.getStaticEffects().getStaticEffect(stAb);
        se.setAffectedCards(affectedCards);
        se.setAffectedPlayers(affectedPlayers);
        se.setParams(params);
        se.setTimestamp(hostCard.getTimestamp());

        String changeColorWordsTo = null;
        Card gainTextSource = null;

        String addP = "";
        int powerBonus = 0;
        String addT = "";
        int toughnessBonus = 0;
        String setP = "";
        int setPower = Integer.MAX_VALUE;
        String setT = "";
        int setToughness = Integer.MAX_VALUE;

        List<String> addKeywords = null;
        List<String> addHiddenKeywords = Lists.newArrayList();
        List<String> removeKeywords = null;
        String[] addAbilities = null;
        String[] addReplacements = null;
        String[] addSVars = null;
        List<String> addTypes = null;
        List<String> removeTypes = null;
        String addColors = null;
        String[] addTriggers = null;
        String[] addStatics = null;
        boolean removeAllAbilities = false;
        boolean removeIntrinsicAbilities = false;
        boolean removeNonMana = false;
        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeLandTypes = false;
        boolean removeCreatureTypes = false;
        boolean removeArtifactTypes = false;
        boolean removeEnchantmentTypes = false;

        boolean overwriteColors = false;

        Set<Keyword> cantHaveKeyword = null;

        List<Player> mayLookAt = null;
        List<Player> withFlash = null;

        boolean controllerMayPlay = false, mayPlayWithoutManaCost = false, mayPlayWithFlash = false;
        String mayPlayAltManaCost = null;
        boolean mayPlayGrantZonePermissions = true;
        Integer mayPlayLimit = null;

        //Global rules changes
        if (layer == StaticAbilityLayer.RULES && params.containsKey("GlobalRule")) {
            final StaticEffects effects = game.getStaticEffects();
            effects.setGlobalRuleChange(GlobalRuleChange.fromString(params.get("GlobalRule")));
        }

        if (layer == StaticAbilityLayer.TEXT && params.containsKey("GainTextOf")) {
            final String valid = params.get("GainTextOf");
            CardCollection allValid = CardLists.getValidCards(game.getCardsInGame(), valid, hostCard.getController(), hostCard, null);
            if (allValid.size() > 1) {
                // TODO: if ever necessary, support gaining text of multiple cards at the same time
                System.err.println("Error: GainTextOf parameter was not defined as a unique card for " + hostCard);
            } else if (allValid.size() == 1) {
                gainTextSource = allValid.get(0);
            } else {
                gainTextSource = null;
            }
        }

        if (layer == StaticAbilityLayer.TEXT && params.containsKey("ChangeColorWordsTo")) {
            changeColorWordsTo = params.get("ChangeColorWordsTo");
        }

        if (layer == StaticAbilityLayer.SETPT &&params.containsKey("SetPower")) {
            setP = params.get("SetPower");
            setPower = AbilityUtils.calculateAmount(hostCard, setP, stAb);
        }

        if (layer == StaticAbilityLayer.SETPT && params.containsKey("SetToughness")) {
            setT = params.get("SetToughness");
            setToughness = AbilityUtils.calculateAmount(hostCard, setT, stAb);
        }

        if (layer == StaticAbilityLayer.MODIFYPT && params.containsKey("AddPower")) {
            addP = params.get("AddPower");
            powerBonus = AbilityUtils.calculateAmount(hostCard, addP, stAb, true);
        }

        if (layer == StaticAbilityLayer.MODIFYPT && params.containsKey("AddToughness")) {
            addT = params.get("AddToughness");
            toughnessBonus = AbilityUtils.calculateAmount(hostCard, addT, stAb, true);
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("AddKeyword")) {
            addKeywords = Lists.newArrayList(Arrays.asList(params.get("AddKeyword").split(" & ")));
            final List<String> newKeywords = Lists.newArrayList();

            // update keywords with Chosen parts
            final String hostCardUID = Integer.toString(hostCard.getId()); // Protection with "doesn't remove" effect

            final ColorSet colorsYouCtrl = CardUtil.getColorsYouCtrl(controller);

            Iterables.removeIf(addKeywords, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    if (!hostCard.hasChosenColor() && input.contains("ChosenColor")) {
                        return true;
                    }
                    if (!hostCard.hasChosenType() && input.contains("ChosenType")) {
                        return true;
                    }
                    if (!hostCard.hasChosenNumber() && input.contains("ChosenNumber")) {
                        return true;
                    }
                    if (!hostCard.hasChosenPlayer() && input.contains("ChosenPlayer")) {
                        return true;
                    }
                    if (!hostCard.hasChosenName() && input.contains("ChosenName")) {
                        return true;
                    }
                    if (!hostCard.hasChosenEvenOdd() && (input.contains("ChosenEvenOdd") || input.contains("chosenEvenOdd"))) {
                        return true;
                    }

                    // two variants for Red vs. red in keyword
                    if (input.contains("ColorsYouCtrl") || input.contains("colorsYouCtrl")) {
                        for (byte color : colorsYouCtrl) {
                            final String colorWord = MagicColor.toLongString(color);
                            String y = input.replaceAll("ColorsYouCtrl", StringUtils.capitalize(colorWord));
                            y = y.replaceAll("colorsYouCtrl", colorWord);
                            newKeywords.add(y);
                        }
                        return true;
                    }

                    return false;
                }

            });

            addKeywords.addAll(newKeywords);

            addKeywords = Lists.transform(addKeywords, new Function<String, String>() {

                @Override
                public String apply(String input) {
                    if (hostCard.hasChosenColor()) {
                        input = input.replaceAll("ChosenColor", StringUtils.capitalize(hostCard.getChosenColor()));
                    }
                    if (hostCard.hasChosenType()) {
                        input = input.replaceAll("ChosenType", hostCard.getChosenType());
                    }
                    if (hostCard.hasChosenNumber()) {
                        input = input.replaceAll("ChosenNumber", String.valueOf(hostCard.getChosenNumber()));
                    }
                    if (hostCard.hasChosenPlayer()) {
                        Player cp = hostCard.getChosenPlayer();
                        input = input.replaceAll("ChosenPlayerUID", String.valueOf(cp.getId()));
                        input = input.replaceAll("ChosenPlayerName", cp.getName());
                    }
                    if (hostCard.hasChosenName()) {
                        final String chosenName = hostCard.getChosenName().replace(",", ";");
                        input = input.replaceAll("ChosenName", "Card.named" + chosenName);
                    }
                    if (hostCard.hasChosenEvenOdd()) {
                        input = input.replaceAll("ChosenEvenOdd", hostCard.getChosenEvenOdd().toString());
                        input = input.replaceAll("chosenEvenOdd", hostCard.getChosenEvenOdd().toString().toLowerCase());
                    }
                    input = input.replace("HostCardUID", hostCardUID);
                    return input;
                }

            });

            if (params.containsKey("SharedKeywordsZone")) {
                List<ZoneType> zones = ZoneType.listValueOf(params.get("SharedKeywordsZone"));
                String[] restrictions = params.containsKey("SharedRestrictions") ? params.get("SharedRestrictions").split(",") : new String[] {"Card"};
                addKeywords = CardFactoryUtil.sharedKeywords(addKeywords, restrictions, zones, hostCard);
            }
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("CantHaveKeyword")) {
            cantHaveKeyword = Keyword.setValueOf(params.get("CantHaveKeyword"));
        }

        if ((layer == StaticAbilityLayer.RULES) && params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords.addAll(Arrays.asList(params.get("AddHiddenKeyword").split(" & ")));
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("RemoveKeyword")) {
            removeKeywords = Arrays.asList(params.get("RemoveKeyword").split(" & "));
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("RemoveAllAbilities")) {
            removeAllAbilities = true;
            if (params.containsKey("ExceptManaAbilities")) {
                removeNonMana = true;
            }
        }
        // do this in type layer too in case of blood moon
        if ((layer == StaticAbilityLayer.TYPE)
                && params.containsKey("RemoveIntrinsicAbilities")) {
            removeIntrinsicAbilities = true;
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("AddAbility")) {
            final String[] sVars = params.get("AddAbility").split(" & ");
            for (int i = 0; i < sVars.length; i++) {
                sVars[i] = hostCard.getSVar(sVars[i]);
            }
            addAbilities = sVars;
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("AddReplacementEffects")) {
            final String[] sVars = params.get("AddReplacementEffects").split(" & ");
            for (int i = 0; i < sVars.length; i++) {
                sVars[i] = hostCard.getSVar(sVars[i]);
            }
            addReplacements = sVars;
        }

        if (layer == StaticAbilityLayer.ABILITIES && params.containsKey("AddSVar")) {
            addSVars = params.get("AddSVar").split(" & ");
        }

        if (layer == StaticAbilityLayer.TYPE && params.containsKey("AddType")) {

            addTypes = Lists.newArrayList(Arrays.asList(params.get("AddType").split(" & ")));
            List<String> newTypes = Lists.newArrayList();

            Iterables.removeIf(addTypes, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    if (input.equals("ChosenType") && !hostCard.hasChosenType()) {
                        return true;
                    }
                    if (input.equals("ImprintedCreatureType")) {
                        if (hostCard.hasImprintedCard()) {
                            newTypes.addAll(hostCard.getImprintedCards().getLast().getType().getCreatureTypes());
                        }
                        return true;
                    }
                    if (input.equals("AllBasicLandType")) {
                        newTypes.addAll(CardType.getBasicTypes());
                        return true;
                    }
                    return false;
                }
            });
            addTypes.addAll(newTypes);

            addTypes = Lists.transform(addTypes, new Function<String, String>() {
                @Override
                public String apply(String input) {
                    if (hostCard.hasChosenType()) {
                        input = input.replaceAll("ChosenType", hostCard.getChosenType());
                    }
                    return input;
                }

            });
        }

        if (layer == StaticAbilityLayer.TYPE && params.containsKey("RemoveType")) {
            removeTypes = Lists.newArrayList(Arrays.asList(params.get("RemoveType").split(" & ")));

            Iterables.removeIf(removeTypes, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    if (input.equals("ChosenType") && !hostCard.hasChosenType()) {
                        return true;
                    }
                    return false;
                }
            });
        }

        if (layer == StaticAbilityLayer.TYPE) {
            if (params.containsKey("RemoveSuperTypes")) {
                removeSuperTypes = true;
            }

            if (params.containsKey("RemoveCardTypes")) {
                removeCardTypes = true;
            }

            if (params.containsKey("RemoveSubTypes")) {
                removeSubTypes = true;
            }

            if (params.containsKey("RemoveLandTypes")) {
                removeLandTypes = true;
            }
            if (params.containsKey("RemoveCreatureTypes")) {
                removeCreatureTypes = true;
            }
            if (params.containsKey("RemoveArtifactTypes")) {
                removeArtifactTypes = true;
            }
            if (params.containsKey("RemoveEnchantmentTypes")) {
                removeEnchantmentTypes = true;
            }
        }

        if (layer == StaticAbilityLayer.COLOR) {
            if (params.containsKey("AddColor")) {
                final String colors = params.get("AddColor");
                if (colors.equals("ChosenColor")) {
                    addColors = CardUtil.getShortColorsString(hostCard.getChosenColors());
                } else if (colors.equals("All")) {
                    addColors = "W U B R G";
                } else {
                    addColors = CardUtil.getShortColorsString(Arrays.asList(colors.split(" & ")));
                }
            }

            if (params.containsKey("SetColor")) {
                final String colors = params.get("SetColor");
                if (colors.equals("ChosenColor")) {
                    addColors = CardUtil.getShortColorsString(hostCard.getChosenColors());
                } else if (colors.equals("All")) {
                    addColors = "W U B R G";
                } else {
                    addColors = CardUtil.getShortColorsString(Arrays.asList(colors.split(" & ")));
                }
                overwriteColors = true;
            }
        }

        if (layer == StaticAbilityLayer.ABILITIES) {
            if (params.containsKey("AddTrigger")) {
                final String[] sVars = params.get("AddTrigger").split(" & ");
                for (int i = 0; i < sVars.length; i++) {
                    sVars[i] = hostCard.getSVar(sVars[i]);
                }
                addTriggers = sVars;
            }

            if (params.containsKey("AddStaticAbility")) {
                final String[] sVars = params.get("AddStaticAbility").split(" & ");
                for (int i = 0; i < sVars.length; i++) {
                    sVars[i] = hostCard.getSVar(sVars[i]);
                }
                addStatics = sVars;
            }
        }

        if (layer == StaticAbilityLayer.RULES) {
            // These fall under Rule changes, as they don't fit any other category
            if (params.containsKey("MayLookAt")) {
                String look = params.get("MayLookAt");
                if ("True".equals(look)) {
                    look = "You";
                }
                mayLookAt = AbilityUtils.getDefinedPlayers(hostCard, look, null);
            }
            if (params.containsKey("MayPlay")) {
                controllerMayPlay = true;
                if (params.containsKey("MayPlayWithoutManaCost")) {
                    mayPlayWithoutManaCost = true;
                } else if (params.containsKey("MayPlayAltManaCost")) {
                    mayPlayAltManaCost = params.get("MayPlayAltManaCost");
                }
                if (params.containsKey("MayPlayWithFlash")) {
                	mayPlayWithFlash = true;
                }
                if (params.containsKey("MayPlayLimit")) {
                    mayPlayLimit = Integer.parseInt(params.get("MayPlayLimit"));
                }
                if (params.containsKey("MayPlayDontGrantZonePermissions")) {
                    mayPlayGrantZonePermissions = false;
                }
            }
            if (params.containsKey("WithFlash")) {
                withFlash = AbilityUtils.getDefinedPlayers(hostCard, params.get("WithFlash"), null);
            }

            if (params.containsKey("IgnoreEffectCost")) {
                String cost = params.get("IgnoreEffectCost");
                buildIgnorEffectAbility(stAb, cost, affectedPlayers, affectedCards);
            }
        }

        // modify players
        for (final Player p : affectedPlayers) {

            // add keywords
            if (addKeywords != null) {
                p.addChangedKeywords(addKeywords, removeKeywords, se.getTimestamp());
            }

            // add static abilities
            if (addStatics != null) {
                for (String s : addStatics) {
                    StaticAbility stat = p.addStaticAbility(hostCard, s);
                    stat.setIntrinsic(false);
                }
            }

            if (layer == StaticAbilityLayer.RULES) {
                if (params.containsKey("SetMaxHandSize")) {
                    String mhs = params.get("SetMaxHandSize");
                    if (mhs.equals("Unlimited")) {
                        p.setUnlimitedHandSize(true);
                    } else {
                        int max = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                        p.setMaxHandSize(max);
                    }
                }

                if (params.containsKey("AdjustLandPlays")) {
                    String mhs = params.get("AdjustLandPlays");
                    if (mhs.equals("Unlimited")) {
                        p.addMaxLandPlaysInfinite(se.getTimestamp());
                    } else {
                        int add = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                        p.addMaxLandPlays(se.getTimestamp(), add);
                    }
                }

                if (params.containsKey("ControlVote")) {
                    p.addControlVote(se.getTimestamp());
                }
                if (params.containsKey("AdditionalVote")) {
                    String mhs = params.get("AdditionalVote");
                    int add = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                    p.addAdditionalVote(se.getTimestamp(), add);
                }
                if (params.containsKey("AdditionalOptionalVote")) {
                    String mhs = params.get("AdditionalOptionalVote");
                    int add = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                    p.addAdditionalOptionalVote(se.getTimestamp(), add);
                }

                if (params.containsKey("RaiseMaxHandSize")) {
                    String rmhs = params.get("RaiseMaxHandSize");
                    int rmax = AbilityUtils.calculateAmount(hostCard, rmhs, stAb);
                    p.setMaxHandSize(p.getMaxHandSize() + rmax);
                }

                if (params.containsKey("ManaColorConversion")) {
                    AbilityUtils.applyManaColorConversion(p.getManaPool(), params);
                }
            }
        }

        // start modifying the cards
        for (int i = 0; i < affectedCards.size(); i++) {
            final Card affectedCard = affectedCards.get(i);

            // Gain control
            if (layer == StaticAbilityLayer.CONTROL && params.containsKey("GainControl")) {
                affectedCard.addTempController(hostCard.getController(), hostCard.getTimestamp());
            }

            // Gain text from another card
            if (layer == StaticAbilityLayer.TEXT) {
                if (gainTextSource != null) {
                    affectedCard.addTextChangeState(
                        CardFactory.getCloneStates(gainTextSource, affectedCard, stAb), se.getTimestamp()
                    );
                }
            }

            // Change color words
            if (changeColorWordsTo != null) {
                final byte color;
                if (changeColorWordsTo.equals("ChosenColor")) {
                    if (hostCard.hasChosenColor()) {
                        color = MagicColor.fromName(Iterables.getFirst(hostCard.getChosenColors(), null));
                    } else {
                        color = 0;
                    }
                } else {
                    color = MagicColor.fromName(changeColorWordsTo);
                }

                if (color != 0) {
                    final String colorName = MagicColor.toLongString(color);
                    affectedCard.addChangedTextColorWord("Any", colorName, se.getTimestamp());
                }
            }

            // set P/T
            if (layer == StaticAbilityLayer.SETPT) {
                if ((setPower != Integer.MAX_VALUE) || (setToughness != Integer.MAX_VALUE)) {
                    // non CharacteristicDefining
                    if (setP.startsWith("AffectedX")) {
                        setPower = CardFactoryUtil.xCount(affectedCard, AbilityUtils.getSVar(stAb, setP));
                    }
                    if (setT.startsWith("AffectedX")) {
                        setToughness = CardFactoryUtil.xCount(affectedCard, AbilityUtils.getSVar(stAb, setT));
                    }
                    affectedCard.addNewPT(setPower, setToughness,
                        hostCard.getTimestamp(), stAb.hasParam("CharacteristicDefining"));
                }
            }

            // add P/T bonus
            if (layer == StaticAbilityLayer.MODIFYPT) {
                if (addP.startsWith("AffectedX")) {
                    powerBonus = CardFactoryUtil.xCount(affectedCard, AbilityUtils.getSVar(stAb, addP));
                }
                if (addT.startsWith("AffectedX")) {
                    toughnessBonus = CardFactoryUtil.xCount(affectedCard, AbilityUtils.getSVar(stAb, addT));
                }
                affectedCard.addPTBoost(powerBonus, toughnessBonus, se.getTimestamp(), stAb.getId());
            }

            // add keywords
            // TODO regular keywords currently don't try to use keyword multiplier
            // (Although nothing uses it at this time)
            if ((addKeywords != null) || (removeKeywords != null) || removeAllAbilities || removeIntrinsicAbilities) {
                List<String> newKeywords = null;
                if (addKeywords != null) {
                    newKeywords = Lists.newArrayList(addKeywords);
                    final List<String> extraKeywords = Lists.newArrayList();

                    Iterables.removeIf(newKeywords, new Predicate<String>() {
                        @Override
                        public boolean apply(String input) {
                            if (input.contains("CardManaCost")) {
                                if (affectedCard.getManaCost().isNoCost()) {
                                    return true;
                                }
                            }
                            // replace one Keyword with list of keywords
                            if (input.startsWith("Protection") && input.contains("CardColors")) {
                                for (Byte color : affectedCard.determineColor()) {
                                    extraKeywords.add(input.replace("CardColors", MagicColor.toLongString(color)));
                                }
                                return true;
                            }
                            return false;
                        }
                    });
                    newKeywords.addAll(extraKeywords);

                    newKeywords = Lists.transform(newKeywords, new Function<String, String>() {

                        @Override
                        public String apply(String input) {
                            if (input.contains("CardManaCost")) {
                                input = input.replace("CardManaCost", affectedCard.getManaCost().getShortString());
                            } else if (input.contains("ConvertedManaCost")) {
                                final String costcmc = Integer.toString(affectedCard.getCMC());
                                input = input.replace("ConvertedManaCost", costcmc);
                            }
                            return input;
                        }
                    });
                }

                affectedCard.addChangedCardKeywords(newKeywords, removeKeywords,
                        removeAllAbilities, removeIntrinsicAbilities,
                        hostCard.getTimestamp());
            }

            // add HIDDEN keywords
            if (!addHiddenKeywords.isEmpty()) {
                for (final String k : addHiddenKeywords) {
                    affectedCard.addHiddenExtrinsicKeyword(k);
                }
            }

            // add SVars
            if (addSVars != null) {
                for (final String sVar : addSVars) {
                    String actualSVar = hostCard.getSVar(sVar);
                    String name = sVar;
                    if (actualSVar.startsWith("SVar:")) {
                        actualSVar = actualSVar.split("SVar:")[1];
                        name = actualSVar.split(":")[0];
                        actualSVar = actualSVar.split(":")[1];
                    }
                    affectedCard.setSVar(name, actualSVar);
                }
            }

            if (layer == StaticAbilityLayer.ABILITIES) {

                List<SpellAbility> addedAbilities = Lists.newArrayList();
                List<ReplacementEffect> addedReplacementEffects = Lists.newArrayList();
                List<Trigger> addedTrigger = Lists.newArrayList();
                List<StaticAbility> addedStaticAbility = Lists.newArrayList();
                // add abilities
                if (addAbilities != null) {
                    for (String abilty : addAbilities) {
                        if (abilty.contains("CardManaCost")) {
                            abilty = TextUtil.fastReplace(abilty, "CardManaCost", affectedCard.getManaCost().getShortString());
                        } else if (abilty.contains("ConvertedManaCost")) {
                            final String costcmc = Integer.toString(affectedCard.getCMC());
                            abilty = TextUtil.fastReplace(abilty, "ConvertedManaCost", costcmc);
                        }
                        if (abilty.startsWith("AB") || abilty.startsWith("ST")) { // grant the ability
                            final SpellAbility sa = AbilityFactory.getAbility(abilty, affectedCard);
                            sa.setIntrinsic(false);
                            sa.setOriginalHost(hostCard);
                            addedAbilities.add(sa);
                        }
                    }
                }

                if (params.containsKey("GainsAbilitiesOf")) {
                    final String[] valids = params.get("GainsAbilitiesOf").split(",");
                    List<ZoneType> validZones;
                    final boolean loyaltyAB = params.containsKey("GainsLoyaltyAbilities");
                    if (params.containsKey("GainsAbilitiesOfZones")) {
                        validZones = ZoneType.listValueOf(params.get("GainsAbilitiesOfZones"));
                    } else {
                        validZones = ImmutableList.of(ZoneType.Battlefield);
                    }

                    CardCollectionView cardsIGainedAbilitiesFrom = game.getCardsIn(validZones);
                    cardsIGainedAbilitiesFrom = CardLists.getValidCards(cardsIGainedAbilitiesFrom, valids, hostCard.getController(), hostCard, null);

                    for (Card c : cardsIGainedAbilitiesFrom) {
                        for (SpellAbility sa : c.getSpellAbilities()) {
                            if (sa instanceof AbilityActivated) {
                                if (loyaltyAB && !sa.isPwAbility()) {
                                    continue;
                                }
                                SpellAbility newSA = sa.copy(affectedCard, false);
                                if (params.containsKey("GainsAbilitiesLimitPerTurn")) {
                                    newSA.setRestrictions(sa.getRestrictions());
                                    newSA.getRestrictions().setLimitToCheck(params.get("GainsAbilitiesLimitPerTurn"));
                                }
                                if (newSA.getOriginalHost() == null) {
                                    newSA.setOriginalHost(c);
                                }
                                newSA.setOriginalAbility(sa); // need to be set to get the Once Per turn Clause correct
                                newSA.setGrantorStatic(stAb);
                                newSA.setIntrinsic(false);
                                addedAbilities.add(newSA);
                            }
                        }
                    }
                }

                // add Replacement effects
                if (addReplacements != null) {
                    for (String rep : addReplacements) {
                        final ReplacementEffect actualRep = ReplacementHandler.parseReplacement(rep, affectedCard, false);
                        actualRep.setIntrinsic(false);
                        addedReplacementEffects.add(actualRep);
                    }
                }

                // add triggers
                if (addTriggers != null) {
                    for (final String trigger : addTriggers) {
                        final Trigger actualTrigger = TriggerHandler.parseTrigger(trigger, affectedCard, false);
                        // if the trigger has Execute param, which most trigger gained by Static Abilties should have
                        // turn them into SpellAbility object before adding to card
                        // with that the TargetedCard does not need the Svars added to them anymore
                        // but only do it if the trigger doesn't already have a overriding ability
                        if (actualTrigger.hasParam("Execute") && actualTrigger.getOverridingAbility() == null) {
                            SpellAbility sa = AbilityFactory.getAbility(hostCard, actualTrigger.getParam("Execute"));
                            // set hostcard there so when the card is added to trigger, it doesn't make a copy of it
                            sa.setHostCard(affectedCard);
                            // set OriginalHost to get the owner of this static ability
                            sa.setOriginalHost(hostCard);
                            // set overriding ability to the trigger
                            actualTrigger.setOverridingAbility(sa);
                        }
                        actualTrigger.setIntrinsic(false);
                        addedTrigger.add(actualTrigger);
                    }
                }

                // add static abilities
                if (addStatics != null) {
                    for (String s : addStatics) {
                        if (s.contains("ConvertedManaCost")) {
                            final String costcmc = Integer.toString(affectedCard.getCMC());
                            s = TextUtil.fastReplace(s, "ConvertedManaCost", costcmc);
                        }

                        StaticAbility stat = new StaticAbility(s, affectedCard);
                        stat.setIntrinsic(false);
                        addedStaticAbility.add(stat);
                    }
                }

                if (!addedAbilities.isEmpty() || addReplacements != null || addTriggers != null || addStatics != null
                    || removeAllAbilities) {
                    affectedCard.addChangedCardTraits(addedAbilities, null, addedTrigger, addedReplacementEffects, addedStaticAbility, removeAllAbilities, removeNonMana, false, hostCard.getTimestamp());
                }

                if (cantHaveKeyword != null) {
                    affectedCard.addCantHaveKeyword(hostCard.getTimestamp(), cantHaveKeyword);
                }
            }

            if (layer == StaticAbilityLayer.TYPE && removeIntrinsicAbilities) {
                affectedCard.addChangedCardTraits(null, null, null, null, null, false, false, removeIntrinsicAbilities, hostCard.getTimestamp());
            }

            // add Types
            if ((addTypes != null) || (removeTypes != null)) {
                affectedCard.addChangedCardTypes(addTypes, removeTypes, removeSuperTypes, removeCardTypes,
                        removeSubTypes, removeLandTypes, removeCreatureTypes, removeArtifactTypes,
                        removeEnchantmentTypes, hostCard.getTimestamp());
            }

            // add colors
            if (addColors != null) {
                affectedCard.addColor(addColors, !overwriteColors, hostCard.getTimestamp());
            }

            if (layer == StaticAbilityLayer.RULES) {
                if (params.containsKey("Goad")) {
                    affectedCard.addGoad(se.getTimestamp(), hostCard.getController());
                }
                if (params.containsKey("CanBlockAny")) {
                    affectedCard.addCanBlockAny(se.getTimestamp());
                }
                if (params.containsKey("CanBlockAmount")) {
                    int v = AbilityUtils.calculateAmount(hostCard, params.get("CanBlockAmount"), stAb, true);
                    affectedCard.addCanBlockAdditional(v, se.getTimestamp());
                }
            }

            if (mayLookAt != null) {
                for (Player p : mayLookAt) {
                    affectedCard.setMayLookAt(p, true);
                }
            }
            if (withFlash != null) {
                affectedCard.addWithFlash(se.getTimestamp(), withFlash);
            }

            if (controllerMayPlay && (mayPlayLimit == null || stAb.getMayPlayTurn() < mayPlayLimit)) {
                String mayPlayAltCost = mayPlayAltManaCost;

                if (mayPlayAltCost != null && mayPlayAltCost.contains("ConvertedManaCost")) {
                    final String costcmc = Integer.toString(affectedCard.getCMC());
                    mayPlayAltCost = mayPlayAltCost.replace("ConvertedManaCost", costcmc);
                }

                Player mayPlayController = params.containsKey("MayPlayCardOwner") ? affectedCard.getOwner() : controller;
                affectedCard.setMayPlay(mayPlayController, mayPlayWithoutManaCost,
                        mayPlayAltCost != null ? new Cost(mayPlayAltCost, false) : null,
                        mayPlayWithFlash, mayPlayGrantZonePermissions, stAb);
            }
        }

        return affectedCards;
    }

    private static void buildIgnorEffectAbility(final StaticAbility stAb, final String costString, final List<Player> players, final CardCollectionView cards) {
        final List<Player> validActivator = new ArrayList<>(players);
        for (final Card c : cards) {
            validActivator.add(c.getController());
        }
        final Card sourceCard = stAb.getHostCard();
        Cost cost = new Cost(costString, true);
        final AbilityStatic addIgnore = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                stAb.addIgnoreEffectPlayers(this.getActivatingPlayer());
                stAb.setIgnoreEffectCards(cards);
            }

            @Override
            public boolean canPlay() {
                return validActivator.contains(this.getActivatingPlayer())
                        && sourceCard.isInPlay();
            }

        };

        addIgnore.setIntrinsic(false);
        addIgnore.setApi(ApiType.InternalIgnoreEffect);
        addIgnore.setDescription(cost + " Ignore the effect until end of turn.");
        sourceCard.addChangedCardTraits(ImmutableList.of(addIgnore), null, null, null, null, false, false, false, sourceCard.getTimestamp());

        final GameCommand removeIgnore = new GameCommand() {
            private static final long serialVersionUID = -5415775215053216360L;
            @Override
            public void run() {
                stAb.clearIgnoreEffects();
            }
        };
        sourceCard.getGame().getEndOfTurn().addUntil(removeIgnore);
        sourceCard.addLeavesPlayCommand(removeIgnore);
    }

    private static List<Player> getAffectedPlayers(final StaticAbility stAb) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        final List<Player> players = new ArrayList<>();

        if (!params.containsKey("Affected")) {
            return players;
        }

        final String[] strngs = params.get("Affected").split(",");

        for (Player p : controller.getGame().getPlayersInTurnOrder()) {
            if (p.isValid(strngs, controller, hostCard, null)) {
                players.add(p);
            }
        }
        players.removeAll(stAb.getIgnoreEffectPlayers());

        return players;
    }

    private static CardCollectionView getAffectedCards(final StaticAbility stAb, final CardCollectionView preList) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Game game = hostCard.getGame();
        final Player controller = hostCard.getController();

        if (params.containsKey("CharacteristicDefining")) {
            return new CardCollection(hostCard); // will always be the card itself
        }

        // non - CharacteristicDefining
        CardCollection affectedCards = new CardCollection();

        // add preList in addition to the normal affected cards
        // need to add before game cards to have preference over them
        if (!preList.isEmpty()) {
            if (params.containsKey("AffectedZone")) {
                affectedCards.addAll(CardLists.filter(preList, CardPredicates.inZone(
                        ZoneType.listValueOf(params.get("AffectedZone")))));
            } else {
                affectedCards.addAll(CardLists.filter(preList, CardPredicates.inZone(ZoneType.Battlefield)));
            }
        }

        if (params.containsKey("AffectedZone")) {
            affectedCards.addAll(game.getCardsIn(ZoneType.listValueOf(params.get("AffectedZone"))));
        } else {
            affectedCards.addAll(game.getCardsIn(ZoneType.Battlefield));
        }

        if (params.containsKey("Affected") && !params.get("Affected").contains(",")) {
            if (params.get("Affected").contains("Self")) {
                affectedCards = new CardCollection(hostCard);
            } else if (params.get("Affected").contains("EnchantedBy")) {
                affectedCards = new CardCollection(hostCard.getEnchantingCard());
            } else if (params.get("Affected").contains("EquippedBy")) {
                affectedCards = new CardCollection(hostCard.getEquipping());
            } else if (params.get("Affected").equals("EffectSource")) {
                affectedCards = new CardCollection(AbilityUtils.getDefinedCards(hostCard, params.get("Affected"), null));
                return affectedCards;
            }
        }

        if (params.containsKey("Affected")) {
            affectedCards = CardLists.getValidCards(affectedCards, params.get("Affected").split(","), controller, hostCard, null);
        }
        affectedCards.removeAll((List<?>) stAb.getIgnoreEffectCards());
        return affectedCards;
    }
}
