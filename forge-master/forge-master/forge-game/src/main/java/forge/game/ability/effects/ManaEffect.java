package forge.game.ability.effects;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import java.util.List;

public class ManaEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        AbilityManaPart abMana = sa.getManaPart();

        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable());

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final boolean optional = sa.hasParam("Optional");
        final Game game = sa.getActivatingPlayer().getGame();

        if (optional && !sa.getActivatingPlayer().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantAddMana"))) {
            return;
        }

        if (sa.hasParam("DoubleManaInPool")) {
            for (final Player player : tgtPlayers) {
                for (byte color : ManaAtom.MANATYPES) {
                    int amountColor = player.getManaPool().getAmountOfColor(color);
                    for (int i = 0; i < amountColor; i++) {
                        abMana.produceMana(MagicColor.toShortString(color), player, sa);
                    }
                }
            }
        }

        if (sa.hasParam("ProduceNoOtherMana")) {
            return;
        }

        if (abMana.isComboMana()) {
            for (Player p : tgtPlayers) {
                int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa) : 1;
                if (tgt != null && !p.canBeTargetedBy(sa)) {
                    // Illegal target. Skip.
                    continue;
                }

                Player activator = sa.getActivatingPlayer();
                String express = abMana.getExpressChoice();
                String[] colorsProduced = abMana.getComboColors().split(" ");

                final StringBuilder choiceString = new StringBuilder();
                ColorSet colorOptions = null;
                String[] colorsNeeded = express.isEmpty() ? null : express.split(" ");
                if (!abMana.isAnyMana()) {
                    colorOptions = ColorSet.fromNames(colorsProduced);
                } else {
                    colorOptions = ColorSet.fromNames(MagicColor.Constant.ONLY_COLORS);
                }
                boolean differentChoice = abMana.getOrigProduced().contains("Different");
                ColorSet fullOptions = colorOptions;
                for (int nMana = 0; nMana < amount; nMana++) {
                    String choice = "";
                    if (colorsNeeded != null && colorsNeeded.length > nMana) {	// select from express choices if possible
                        colorOptions = ColorSet
                                .fromMask(fullOptions.getColor() & ManaAtom.fromName(colorsNeeded[nMana]));
                    }
                    if (colorOptions.isColorless() && colorsProduced.length > 0) {
                        // If we just need generic mana, no reason to ask the controller for a choice,
                        // just use the first possible color.
                        choice = colorsProduced[differentChoice ? nMana : 0];
                    } else {
                        byte chosenColor = activator.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa,
                                differentChoice ? fullOptions : colorOptions);
                        if (chosenColor == 0)
                            throw new RuntimeException("ManaEffect::resolve() /*combo mana*/ - " + activator + " color mana choice is empty for " + card.getName());
                        
                        fullOptions = ColorSet.fromMask(fullOptions.getMyColor() - chosenColor);
                        choice = MagicColor.toShortString(chosenColor);
                    }
                    
                    if (nMana > 0) {
                        choiceString.append(" ");
                    }
                    choiceString.append(choice);
                }

                if (choiceString.toString().isEmpty() && "Combo ColorIdentity".equals(abMana.getOrigProduced())) {
                    // No mana could be produced here (non-EDH match?), so cut short
                    return;
                }

                game.action.nofityOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", activator.getName(), choiceString), activator);
                abMana.setExpressChoice(choiceString.toString());
            }
        }
        else if (abMana.isAnyMana()) {
            for (Player p : tgtPlayers) {
                if (tgt != null && !p.canBeTargetedBy(sa)) {
                    // Illegal target. Skip.
                    continue;
                }

                Player act = sa.getActivatingPlayer();
                // AI color choice is set in ComputerUtils so only human players need to make a choice

                String colorsNeeded = abMana.getExpressChoice();
                String choice = "";

                ColorSet colorMenu = null;
                byte mask = 0;
                //loop through colors to make menu
                for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                    mask |= MagicColor.fromName(colorsNeeded.charAt(nChar));
                }
                colorMenu = mask == 0 ? ColorSet.ALL_COLORS : ColorSet.fromMask(mask);
                byte val = p.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, colorMenu);
                if (0 == val) {
                    throw new RuntimeException("ManaEffect::resolve() /*any mana*/ - " + act + " color mana choice is empty for " + card.getName());
                }
                choice = MagicColor.toShortString(val);

                game.action.nofityOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", act.getName(), choice), act);
                abMana.setExpressChoice(choice);
            }
        }
        else if (abMana.isSpecialMana()) {
            for (Player p : tgtPlayers) {
                if (tgt != null && !p.canBeTargetedBy(sa)) {
                    // Illegal target. Skip.
                    continue;
                }

                String type = abMana.getOrigProduced().split("Special ")[1];

                if (type.equals("EnchantedManaCost")) {
                    Card enchanted = card.getEnchantingCard();
                    if (enchanted == null ) 
                        continue;

                    StringBuilder sb = new StringBuilder();
                    int generic = enchanted.getManaCost().getGenericCost();
                    if( generic > 0 )
                        sb.append(generic);

                    for (ManaCostShard s : enchanted.getManaCost()) {
                        ColorSet cs = ColorSet.fromMask(s.getColorMask());
                        if(cs.isColorless())
                            continue;
                        sb.append(' ');
                        if (cs.isMonoColor())
                            sb.append(MagicColor.toShortString(s.getColorMask()));
                        else /* (cs.isMulticolor()) */ {
                            byte chosenColor = sa.getActivatingPlayer().getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                            sb.append(MagicColor.toShortString(chosenColor));
                        }
                    }
                    abMana.setExpressChoice(sb.toString().trim());
                } else if (type.equals("LastNotedType")) {
                    Mana manaType = (Mana) Iterables.getFirst(card.getRemembered(), null);
                    if (manaType == null) {
                        return;
                    }
                    String  cs = manaType.toString();
                    abMana.setExpressChoice(cs);
                } else if (type.startsWith("EachColorAmong")) {
                    final String res = type.split("_")[1];
                    final CardCollection list = CardLists.getValidCards(card.getGame().getCardsIn(ZoneType.Battlefield),
                            res, sa.getActivatingPlayer(), card, sa);
                    byte colors = 0;
                    for (Card c : list) {
                        colors |= c.determineColor().getColor();
                    }
                    if (colors == 0) return;
                    abMana.setExpressChoice(ColorSet.fromMask(colors));
                }

                if (abMana.getExpressChoice().isEmpty()) {
                    System.out.println("AbilityFactoryMana::manaResolve() - special mana effect is empty for " + sa.getHostCard().getName());
                }
            }    
        }

        for (final Player player : tgtPlayers) {
            abMana.produceMana(GameActionUtil.generatedMana(sa), player, sa);
        }

        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();

        //resolveDrawback(sa);
    }

    /**
     * <p>
     * manaStackDescription.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a {@link java.lang.String} object.
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String mana = !sa.hasParam("Amount") || StringUtils.isNumeric(sa.getParam("Amount"))
                ? GameActionUtil.generatedMana(sa) : "mana";
        sb.append("Add ").append(mana).append(".");
        return sb.toString();
    }
}
