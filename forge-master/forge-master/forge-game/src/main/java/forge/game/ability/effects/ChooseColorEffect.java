package forge.game.ability.effects;

import forge.card.MagicColor;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooseColorEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (sa.hasParam("OrColors")) {
            sb.append(" or colors");
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        List<String> colorChoices = new ArrayList<>(MagicColor.Constant.ONLY_COLORS);
        if (sa.hasParam("Choices")) {
            String[] restrictedChoices = sa.getParam("Choices").split(",");
            colorChoices = Arrays.asList(restrictedChoices);
        }

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                List<String> chosenColors;
                int cntMin = sa.hasParam("TwoColors") ? 2 : 1;
                int cntMax = sa.hasParam("TwoColors") ? 2 : sa.hasParam("OrColors") ? colorChoices.size() : 1;
                String prompt = null;
                if (cntMax == 1) {
                    prompt = Localizer.getInstance().getMessage("lblChooseAColor");
                }
                else {
                    if (cntMax > cntMin) {
                        if (cntMax >= MagicColor.NUMBER_OR_COLORS) {
                            prompt = Localizer.getInstance().getMessage("lblAtLastChooseNumColors", Lang.getNumeral(cntMin));
                        } else {
                            prompt = Localizer.getInstance().getMessage("lblChooseSpecifiedRangeColors", Lang.getNumeral(cntMin), Lang.getNumeral(cntMax));
                        }
                    }
                    else {
                        prompt = Localizer.getInstance().getMessage("lblChooseNColors", Lang.getNumeral(cntMax));
                    }
                }
                chosenColors = p.getController().chooseColors(prompt, sa, cntMin, cntMax, colorChoices);
                if (chosenColors.isEmpty()) {
                    return;
                }
                card.setChosenColors(chosenColors);
                p.getGame().getAction().nofityOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), Lang.joinHomogenous(chosenColors)), p);
            }
        }
    }
}
