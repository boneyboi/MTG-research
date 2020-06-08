package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import forge.util.PredicateString.StringOp;
import forge.util.Localizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Arrays;
import java.util.List;

public class CopyPermanentEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("Populate")) {
            return "Populate. (Create a token that's a copy of a creature token you control.)";
        }
        final StringBuilder sb = new StringBuilder();


        final List<Card> tgtCards = getTargetCards(sa);

        sb.append("Copy ");
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = host.getGame();
        final List<String> pumpKeywords = Lists.newArrayList();

        if (sa.hasParam("Optional")) {
            if (!activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblCopyPermanentConfirm"))) {
                return;
            }
        }

        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }

        final int numCopies = sa.hasParam("NumCopies") ? AbilityUtils.calculateAmount(host,
                sa.getParam("NumCopies"), sa) : 1;

        Player controller = null;
        if (sa.hasParam("Controller")) {
            final FCollectionView<Player> defined = AbilityUtils.getDefinedPlayers(host, sa.getParam("Controller"), sa);
            if (!defined.isEmpty()) {
                controller = defined.getFirst();
            }
        }
        if (controller == null) {
            controller = activator;
        }

        List<Card> tgtCards = Lists.newArrayList();

        if (sa.hasParam("ValidSupportedCopy")) {
            List<PaperCard> cards = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
            String valid = sa.getParam("ValidSupportedCopy");
            if (valid.contains("X")) {
                valid = TextUtil.fastReplace(valid,
                        "X", Integer.toString(AbilityUtils.calculateAmount(host, "X", sa)));
            }
            if (StringUtils.containsIgnoreCase(valid, "creature")) {
                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (StringUtils.containsIgnoreCase(valid, "equipment")) {
                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.Presets.IS_EQUIPMENT, PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            }
            if (sa.hasParam("RandomCopied")) {
                List<PaperCard> copysource = Lists.newArrayList(cards);
                List<Card> choice = Lists.newArrayList();
                final String num = sa.hasParam("RandomNum") ? sa.getParam("RandomNum") : "1";
                int ncopied = AbilityUtils.calculateAmount(host, num, sa);
                while(ncopied > 0) {
                    final PaperCard cp = Aggregates.random(copysource);
                    Card possibleCard = Card.fromPaperCard(cp, activator); // Need to temporarily set the Owner so the Game is set

                    if (possibleCard.isValid(valid, host.getController(), host, sa)) {
                        choice.add(possibleCard);
                        copysource.remove(cp);
                        ncopied -= 1;
                    }
                }
                tgtCards = choice;
            } else if (sa.hasParam("DefinedName")) {
                String name = sa.getParam("DefinedName");
                if (name.equals("NamedCard")) {
                    if (!host.getNamedCard().isEmpty()) {
                        name = host.getNamedCard();
                    }
                }

                Predicate<PaperCard> cpp = Predicates.compose(CardRulesPredicates.name(StringOp.EQUALS, name), PaperCard.FN_GET_RULES);
                cards = Lists.newArrayList(Iterables.filter(cards, cpp));

                if (!cards.isEmpty()) {
                    tgtCards.add(Card.fromPaperCard(cards.get(0), controller));
                }
            }
        } else if (sa.hasParam("Choices")) {
            Player chooser = activator;
            if (sa.hasParam("Chooser")) {
                final String choose = sa.getParam("Chooser");
                chooser = AbilityUtils.getDefinedPlayers(sa.getHostCard(), choose, sa).get(0);
            }

            CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host);
            if (!choices.isEmpty()) {
                String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard") +" ";

                Card choosen = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, false);

                if (choosen != null) {
                    tgtCards.add(choosen);
                }
            }
        } else {
            tgtCards = getDefinedCardsOrTargeted(sa);
        }

        boolean useZoneTable = true;
        CardZoneTable triggerList = sa.getChangeZoneTable();
        if (triggerList == null) {
            triggerList = new CardZoneTable();
            useZoneTable = false;
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(triggerList);
            useZoneTable = true;
        }

        MutableBoolean combatChanged = new MutableBoolean(false);
        for (final Card c : tgtCards) {
            // if it only targets player, it already got all needed cards from defined
            if (sa.usesTargeting() && !sa.getTargetRestrictions().canTgtPlayer() && !c.canBeTargetedBy(sa)) {
                continue;
            }

            makeTokens(getProtoType(sa, c), controller, sa, numCopies, true, true, triggerList, combatChanged);
        } // end foreach Card

        if (!useZoneTable) {
            triggerList.triggerChangesZoneAll(game);
            triggerList.clear();
        }
        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    } // end resolve

    private Card getProtoType(final SpellAbility sa, final Card original) {
        final Card host = sa.getHostCard();
        final Player newOwner = sa.getActivatingPlayer();
        int id = newOwner == null ? 0 : newOwner.getGame().nextCardId();
        final Card copy = new Card(id, original.getPaperCard(), host.getGame());
        copy.setOwner(newOwner);
        copy.setSetCode(original.getSetCode());

        if (sa.hasParam("Embalm")) {
            copy.setEmbalmed(true);
        }

        if (sa.hasParam("Eternalize")) {
            copy.setEternalized(true);
        }

        copy.setStates(CardFactory.getCloneStates(original, copy, sa));
        // force update the now set State
        copy.setState(copy.getCurrentStateName(), true, true);
        copy.setToken(true);

        return copy;
    }
}
