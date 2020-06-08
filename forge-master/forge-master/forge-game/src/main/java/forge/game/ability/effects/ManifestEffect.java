package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class ManifestEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = source.getGame();
        // Usually a number leaving possibility for X, Sacrifice X land: Manifest X creatures.
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(source,
                sa.getParam("Amount"), sa) : 1;
        // Most commonly "defined" is Top of Library
        final String defined = sa.hasParam("Defined") ? sa.getParam("Defined") : "TopOfLibrary";

        for (final Player p : getTargetPlayers(sa, "DefinedPlayer")) {
            if (sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                CardCollection tgtCards;
                if (sa.hasParam("Choices") || sa.hasParam("ChoiceZone")) {
                    ZoneType choiceZone = ZoneType.Hand;
                    if (sa.hasParam("ChoiceZone")) {
                        choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
                    }
                    CardCollection choices = new CardCollection(game.getCardsIn(choiceZone));
                    if (sa.hasParam("Choices")) {
                        choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, source);
                    }
                    if (choices.isEmpty()) {
                        continue;
                    }

                    String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseCardToManifest") + " ";
                    tgtCards = new CardCollection(activator.getController().chooseEntitiesForEffect(choices, amount, amount, null, sa, title, p));
                } else if ("TopOfLibrary".equals(defined)) {
                    tgtCards = p.getTopXCardsFromLibrary(amount);
                } else {
                    tgtCards = getTargetCards(sa);
                }

                if (sa.hasParam("Shuffle")) {
                    CardLists.shuffle(tgtCards);
                }

                for(Card c : tgtCards) {
                    Card rem = c.manifest(p, sa);
                    if (sa.hasParam("RememberManifested") && rem != null && rem.isManifested()) {
                        source.addRemembered(rem);
                    }
                }
            }
        }
    }
}
