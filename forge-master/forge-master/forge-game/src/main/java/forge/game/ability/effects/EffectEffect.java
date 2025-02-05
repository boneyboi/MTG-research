package forge.game.ability.effects;

import forge.GameCommand;
import forge.ImageKeys;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.util.TextUtil;
import forge.util.collect.FCollection;

public class EffectEffect extends SpellAbilityEffect {

    /**
     * <p>
     * effectResolve.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        String[] effectAbilities = null;
        String[] effectTriggers = null;
        String[] effectSVars = null;
        String[] effectKeywords = null;
        String[] effectStaticAbilities = null;
        String[] effectReplacementEffects = null;
        FCollection<GameObject> rememberList = null;
        String effectImprinted = null;
        List<Player> effectOwner = null;
        boolean imprintOnHost = false;

        if (sa.hasParam("Abilities")) {
            effectAbilities = sa.getParam("Abilities").split(",");
        }

        if (sa.hasParam("Triggers")) {
            effectTriggers = sa.getParam("Triggers").split(",");
        }

        if (sa.hasParam("StaticAbilities")) {
            effectStaticAbilities = sa.getParam("StaticAbilities").split(",");
        }

        if (sa.hasParam("ReplacementEffects")) {
            effectReplacementEffects = sa.getParam("ReplacementEffects").split(",");
        }

        if (sa.hasParam("SVars")) {
            effectSVars = sa.getParam("SVars").split(",");
        }

        if (sa.hasParam("Keywords")) {
            effectKeywords = sa.getParam("Keywords").split(",");
        }

        if (sa.hasParam("RememberObjects")) {
            rememberList = new FCollection<>();
            for (final String rem : sa.getParam("RememberObjects").split(",")) {
                rememberList.addAll(AbilityUtils.getDefinedObjects(hostCard, rem, sa));
            }

            if (sa.hasParam("ForgetCounter")) {
                CounterType cType = CounterType.valueOf(sa.getParam("ForgetCounter"));
                rememberList = new FCollection<GameObject>(CardLists.filter(Iterables.filter(rememberList, Card.class), CardPredicates.hasCounter(cType)));
            }

            // don't create Effect if there is no remembered Objects
            if (rememberList.isEmpty() && (sa.hasParam("ForgetOnMoved") || sa.hasParam("ExileOnMoved") || sa.hasParam("ForgetCounter"))) {
                return;
            }
        }

        if (sa.hasParam("ImprintCards")) {
            effectImprinted = sa.getParam("ImprintCards");
        }

        String name = sa.getParam("Name");
        if (name == null) {
            name = hostCard.getName() + "'s Effect";
        }

        // Unique Effects shouldn't be duplicated
        if (sa.hasParam("Unique") && game.isCardInCommand(name)) {
            return;
        }

        if (sa.hasParam("EffectOwner")) {
            effectOwner = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("EffectOwner"), sa);
        } else {
            effectOwner = Lists.newArrayList(sa.getActivatingPlayer());
        }

        if (sa.hasParam("ImprintOnHost")) {
            imprintOnHost = true;
        }

        String image;
        if (sa.hasParam("Image")) {
            image = ImageKeys.getTokenKey(sa.getParam("Image"));
        } else if (name.startsWith("Emblem")) { // try to get the image from name
            image = ImageKeys.getTokenKey(
            TextUtil.fastReplace(
                TextUtil.fastReplace(
                    TextUtil.fastReplace(name.toLowerCase(), " - ", "_"),
                        ",", ""),
                    " ", "_").toLowerCase());
        } else { // use host image
            image = hostCard.getImageKey();
        }

        for (Player controller : effectOwner) {
            final Card eff = createEffect(sa, controller, name, image);

            // Grant SVars first in order to give references to granted abilities
            if (effectSVars != null) {
                for (final String s : effectSVars) {
                    Card host = sa.getOriginalHost() != null && sa.getHostCard().getSVar(s).isEmpty()
                            && sa.getOriginalHost().hasSVar(s) ? sa.getOriginalHost() : sa.getHostCard();

                    final String actualSVar = host.getSVar(s);
                    eff.setSVar(s, actualSVar);
                }
            }

            // Abilities, triggers and SVars work the same as they do for Token
            // Grant abilities
            if (effectAbilities != null) {
                for (final String s : effectAbilities) {
                    final String actualAbility = AbilityUtils.getSVar(sa, s);

                    final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, eff);
                    eff.addSpellAbility(grantedAbility);
                    grantedAbility.setIntrinsic(true);
                }
            }

            // Grant triggers
            if (effectTriggers != null) {
                for (final String s : effectTriggers) {
                    final String actualTrigger = AbilityUtils.getSVar(sa, s);

                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, eff, true);
                    final String ability = AbilityUtils.getSVar(sa, parsedTrigger.getParam("Execute"));
                    parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(ability, eff));
                    final Trigger addedTrigger = eff.addTrigger(parsedTrigger);
                    addedTrigger.setIntrinsic(true);
                }
            }

            // Grant static abilities
            if (effectStaticAbilities != null) {
                for (final String s : effectStaticAbilities) {
                    final StaticAbility addedStaticAbility = eff.addStaticAbility(AbilityUtils.getSVar(sa, s));
                    if (addedStaticAbility != null) //prevent npe casting adventure card spell
                        addedStaticAbility.setIntrinsic(true);
                }
            }

            // Grant replacement effects
            if (effectReplacementEffects != null) {
                for (final String s : effectReplacementEffects) {
                    final String actualReplacement = AbilityUtils.getSVar(sa, s);

                    final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, eff, true);
                    final ReplacementEffect addedReplacement = eff.addReplacementEffect(parsedReplacement);
                    addedReplacement.setIntrinsic(true);
                }
            }

            // Grant Keywords
            if (effectKeywords != null) {
                for (final String s : effectKeywords) {
                    final String actualKeyword = hostCard.getSVar(s);
                    eff.addIntrinsicKeyword(actualKeyword);
                }
            }

            // Set Remembered
            if (rememberList != null) {
                for (final Object o : rememberList) {
                    eff.addRemembered(o);
                }
                if (sa.hasParam("ForgetOnMoved")) {
                    addForgetOnMovedTrigger(eff, sa.getParam("ForgetOnMoved"));
                } else if (sa.hasParam("ExileOnMoved")) {
                    addExileOnMovedTrigger(eff, sa.getParam("ExileOnMoved"));
                }
                if (sa.hasParam("ForgetCounter")) {
                    addForgetCounterTrigger(eff, sa.getParam("ForgetCounter"));
                }
            }

            // Set Imprinted
            if (effectImprinted != null) {
                for (final Card c : AbilityUtils.getDefinedCards(hostCard, effectImprinted, sa)) {
                    eff.addImprintedCard(c);
                }
            }

            // Set Chosen Color(s)
            if (hostCard.hasChosenColor()) {
                eff.setChosenColors(Lists.newArrayList(hostCard.getChosenColors()));
            }

            // Set Chosen Cards
            if (hostCard.hasChosenCard()) {
                eff.setChosenCards(new CardCollection(hostCard.getChosenCards()));
            }

            // Set Chosen Player
            if (hostCard.getChosenPlayer() != null) {
                eff.setChosenPlayer(hostCard.getChosenPlayer());
            }

            // Set Chosen Type
            if (!hostCard.getChosenType().isEmpty()) {
                eff.setChosenType(hostCard.getChosenType());
            }

            // Set Chosen name
            if (!hostCard.getNamedCard().isEmpty()) {
                eff.setNamedCard(hostCard.getNamedCard());
            }

            // Copy text changes
            if (sa.isIntrinsic()) {
                eff.copyChangedTextFrom(hostCard);
            }

            if (sa.hasParam("AtEOT")) {
                registerDelayedTrigger(sa, sa.getParam("AtEOT"), Lists.newArrayList(hostCard));
            }

            // Duration
            final String duration = sa.getParam("Duration");
            if ((duration == null) || !duration.equals("Permanent")) {
                final GameCommand endEffect = new GameCommand() {
                    private static final long serialVersionUID = -5861759814760561373L;

                    @Override
                    public void run() {
                        game.getAction().exile(eff, null);
                    }
                };

                if ((duration == null) || duration.equals("EndOfTurn")) {
                    game.getEndOfTurn().addUntil(endEffect);
                }
                else if (duration.equals("UntilHostLeavesPlay")) {
                    hostCard.addLeavesPlayCommand(endEffect);
                }
                else if (duration.equals("HostLeavesOrEOT")) {
                    game.getEndOfTurn().addUntil(endEffect);
                    hostCard.addLeavesPlayCommand(endEffect);
                }
                else if (duration.equals("UntilYourNextTurn")) {
                    game.getCleanup().addUntil(controller, endEffect);
                }
                else if (duration.equals("UntilYourNextUpkeep")) {
                    game.getUpkeep().addUntil(controller, endEffect);
                }
                else if (duration.equals("UntilEndOfCombat")) {
                    game.getEndOfCombat().addUntil(endEffect);
                }
                else if (duration.equals("UntilTheEndOfYourNextTurn")) {
                    if (game.getPhaseHandler().isPlayerTurn(controller)) {
                        game.getEndOfTurn().registerUntilEnd(controller, endEffect);
                    } else {
                        game.getEndOfTurn().addUntilEnd(controller, endEffect);
                    }
                }
                else if (duration.equals("ThisTurnAndNextTurn")) {
                    game.getUntap().addAt(new GameCommand() {
                        private static final long serialVersionUID = -5054153666503075717L;

                        @Override
                        public void run() {
                            game.getEndOfTurn().addUntil(endEffect);
                        }
                    });
                }
            }

            if (imprintOnHost) {
                hostCard.addImprintedCard(eff);
            }

            eff.updateStateForView();

            // TODO: Add targeting to the effect so it knows who it's dealing with
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            game.getAction().moveTo(ZoneType.Command, eff, sa);
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
            //if (effectTriggers != null) {
            //    game.getTriggerHandler().registerActiveTrigger(cmdEffect, false);
            //}
        }
    }

}
