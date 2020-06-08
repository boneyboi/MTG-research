package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import java.util.List;

import com.google.common.collect.Lists;

public class MustBlockEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        List<Card> tgtCards = Lists.newArrayList();
        if (sa.hasParam("Choices")) {
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
            tgtCards = getTargetCards(sa);
        }

        final boolean mustBlockAll = sa.hasParam("BlockAllDefined");

        List<Card> cards;
        if (sa.hasParam("DefinedAttacker")) {
            cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa);
        } else {
            cards = Lists.newArrayList(host);
        }

        for (final Card c : tgtCards) {
            if ((!sa.usesTargeting()) || c.canBeTargetedBy(sa)) {
                if (mustBlockAll) {
                    c.addMustBlockCards(cards);
                } else {
                    final Card attacker = cards.get(0);
                    c.addMustBlockCard(attacker);
                    System.out.println(c + " is adding " + attacker + " to mustBlockCards: " + c.getMustBlockCards());
                }
            }
        }

    } // mustBlockResolve()

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();

        // end standard pre-

        String attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa);
            attacker = cards.get(0).toString();
        } else {
            attacker = host.toString();
        }

        if (sa.hasParam("Choices")) {
            sb.append("Choosen creature ").append(" must block ").append(attacker).append(" if able.");
        } else {
            for (final Card c : getTargetCards(sa)) {
                sb.append(c).append(" must block ").append(attacker).append(" if able.");
            }
        }
        return sb.toString();
    }

}
