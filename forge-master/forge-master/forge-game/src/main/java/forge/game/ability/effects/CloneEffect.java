package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.CardTranslation;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class CloneEffect extends SpellAbilityEffect {
    // TODO update this method

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();
        Card tgtCard = host;

        Card cardToCopy = host;
        if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (sa.usesTargeting()) {
            cardToCopy = sa.getTargets().getFirstTargetedCard();
        }

        List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
        if (!cloneTargets.isEmpty()) {
            tgtCard = cloneTargets.get(0);
        }

        sb.append(tgtCard);
        sb.append(" becomes a copy of ").append(cardToCopy).append(".");

        return sb.toString();
    } // end cloneStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        Card tgtCard = host;
        final Game game = activator.getGame();
        final List<String> pumpKeywords = Lists.newArrayList();

        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }

        // find cloning source i.e. thing to be copied
        Card cardToCopy = null;
        
        if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            CardCollection choices = new CardCollection(game.getCardsIn(choiceZone));

            // choices need to be filtered by LastState Battlefield or Graveyard
            // if a Clone enters the field as other cards it could clone,
            // the clone should not be able to clone them
            if (choiceZone.equals(ZoneType.Battlefield)) {
                choices.retainAll(sa.getLastStateBattlefield());
            } else if (choiceZone.equals(ZoneType.Graveyard)) {
                choices.retainAll(sa.getLastStateGraveyard());
            }

            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host);

            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            cardToCopy = activator.getController().chooseSingleEntityForEffect(choices, sa, title, false);
        } else if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (sa.usesTargeting()) {
            cardToCopy = sa.getTargets().getFirstTargetedCard();
        }
        if (cardToCopy == null) {
            return;
        }

        final boolean optional = sa.hasParam("Optional");
        if (optional && !host.getController().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantCopy", CardTranslation.getTranslatedName(cardToCopy.getName())))) {
            return;
        }

        // find target of cloning i.e. card becoming a clone
        if (sa.hasParam("CloneTarget")) {
            final List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
            if (!cloneTargets.isEmpty()) {
                tgtCard = cloneTargets.get(0);
                game.getTriggerHandler().clearInstrinsicActiveTriggers(tgtCard, null);
            }
        } else if (sa.hasParam("Choices") && sa.usesTargeting()) {
            tgtCard = sa.getTargets().getFirstTargetedCard();
            game.getTriggerHandler().clearInstrinsicActiveTriggers(tgtCard, null);
        }

        if (sa.hasParam("CloneZone")) {
            if (!tgtCard.isInZone(ZoneType.smartValueOf(sa.getParam("CloneZone")))) {
                return;
            }
        }

        final Long ts = game.getNextTimestamp();
        tgtCard.addCloneState(CardFactory.getCloneStates(cardToCopy, tgtCard, sa), ts);

        // set ETB tapped of clone
        if (sa.hasParam("IntoPlayTapped")) {
            tgtCard.setTapped(true);
        }

        if (!pumpKeywords.isEmpty()) {
            tgtCard.addChangedCardKeywords(pumpKeywords, Lists.newArrayList(), false, false, ts);
        }

        tgtCard.updateStateForView();

        //Clear Remembered and Imprint lists
        tgtCard.clearRemembered();
        tgtCard.clearImprintedCards();

        // check if clone is now an Aura that needs to be attached
        if (tgtCard.isAura() && !tgtCard.isInZone(ZoneType.Battlefield)) {
            AttachEffect.attachAuraOnIndirectEnterBattlefield(tgtCard);
        }

        if (sa.hasParam("Duration")) {
            final Card cloneCard = tgtCard;
            final GameCommand unclone = new GameCommand() {
                private static final long serialVersionUID = -78375985476256279L;

                @Override
                public void run() {
                    if (cloneCard.removeCloneState(ts)) {
                        cloneCard.updateStateForView();
                        game.fireEvent(new GameEventCardStatsChanged(cloneCard));
                    }
                }
            };

            final String duration = sa.getParam("Duration");
            if (duration.equals("UntilEndOfTurn")) {
                game.getEndOfTurn().addUntil(unclone);
            }
            else if (duration.equals("UntilYourNextTurn")) {
                game.getCleanup().addUntil(host.getController(), unclone);
            }
            else if (duration.equals("UntilUnattached")) {
                sa.getHostCard().addUnattachCommand(unclone);
            }
            else if (duration.equals("UntilFacedown")) {
                sa.getHostCard().addFacedownCommand(unclone);
            }
        }
        if (sa.hasParam("RememberCloneOrigin")) {
            tgtCard.addRemembered(cardToCopy);
        }
        game.fireEvent(new GameEventCardStatsChanged(tgtCard));
    } // cloneResolve

}
