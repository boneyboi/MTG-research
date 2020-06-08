package forge.game;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.util.Expressions;

public class ForgeScript {

    public static boolean cardStateHasProperty(CardState cardState, String property, Player sourceController,
            Card source, SpellAbility spellAbility) {

        final boolean isColorlessSource = cardState.getCard().hasKeyword("Colorless Damage Source", cardState);
        final ColorSet colors = cardState.getCard().determineColor(cardState);
        if (property.contains("White") || property.contains("Blue") || property.contains("Black")
                || property.contains("Red") || property.contains("Green")) {
            boolean mustHave = !property.startsWith("non");
            boolean withSource = property.endsWith("Source");
            if (withSource && isColorlessSource) {
                return false;
            }

            final String colorName = property.substring(mustHave ? 0 : 3, property.length() - (withSource ? 6 : 0));

            int desiredColor = MagicColor.fromName(colorName);
            boolean hasColor = colors.hasAnyColor(desiredColor);
            return mustHave == hasColor;

        } else if (property.contains("Colorless")) { // ... Card is colorless
            boolean non = property.startsWith("non");
            boolean withSource = property.endsWith("Source");
            if (non && withSource && isColorlessSource) {
                return false;
            }
            return non != colors.isColorless();

        } else if (property.contains("MultiColor")) {
            // ... Card is multicolored
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            return property.startsWith("non") != colors.isMulticolor();

        } else if (property.contains("MonoColor")) { // ... Card is monocolored
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            return property.startsWith("non") != colors.isMonoColor();

        } else if (property.startsWith("ChosenColor")) {
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            return source.hasChosenColor() && colors.hasAnyColor(MagicColor.fromName(source.getChosenColor()));

        } else if (property.startsWith("AnyChosenColor")) {
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            return source.hasChosenColor()
                    && colors.hasAnyColor(ColorSet.fromNames(source.getChosenColors()).getColor());

        } else if (property.startsWith("non")) {
            // ... Other Card types
            return !cardState.getTypeWithChanges().hasStringType(property.substring(3));
        } else if (property.equals("CostsPhyrexianMana")) {
            return cardState.getManaCost().hasPhyrexian();
        } else if (property.startsWith("HasSVar")) {
            final String svar = property.substring(8);
            return cardState.hasSVar(svar);
        } else if (property.equals("ChosenType")) {
            return cardState.getTypeWithChanges().hasStringType(source.getChosenType());
        } else if (property.equals("IsNotChosenType")) {
            return !cardState.getTypeWithChanges().hasStringType(source.getChosenType());
        } else if (property.startsWith("HasSubtype")) {
            final String subType = property.substring(11);
            return cardState.getTypeWithChanges().hasSubtype(subType);
        } else if (property.startsWith("HasNoSubtype")) {
            final String subType = property.substring(13);
            return !cardState.getTypeWithChanges().hasSubtype(subType);
        } else if (property.equals("hasActivatedAbilityWithTapCost")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isAbility() && sa.getPayCosts().hasTapCost()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasActivatedAbility")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasManaAbility")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasNonManaActivatedAbility")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isAbility() && !sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("cmc")) {
            int x;
            String rhs = property.substring(5);
            int y = cardState.getManaCost().getCMC();
            try {
                x = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                x = AbilityUtils.calculateAmount(source, rhs, spellAbility);
            }

            return Expressions.compare(y, property, x);
        } else return cardState.getTypeWithChanges().hasStringType(property);


    }


    public static boolean spellAbilityHasProperty(SpellAbility sa, String property, Player sourceController,
            Card source, SpellAbility spellAbility) {
        if (property.equals("ManaAbility")) {
            return sa.isManaAbility();
        } else if (property.equals("nonManaAbility")) {
            return !sa.isManaAbility();
        } else if (property.equals("withoutXCost")) {
            return !sa.isXCost();
        } else if (property.equals("Buyback")) {
            return sa.isBuyBackAbility();
        } else if (property.equals("Cycling")) {
            return sa.isCycling();
        } else if (property.equals("Dash")) {
            return sa.isDash();
        } else if (property.equals("Flashback")) {
            return sa.isFlashBackAbility();
        } else if (property.equals("Jumpstart")) {
            return sa.isJumpstart();
        } else if (property.equals("Kicked")) {
            return sa.isKicked();
        } else if (property.equals("Loyalty")) {
            return sa.isPwAbility();
        } else if (property.equals("Aftermath")) {
            return sa.isAftermath();
        } else if (property.equals("MorphUp")) {
            return sa.isMorphUp();
        } else if (property.equals("Equip")) {
            return sa.hasParam("Equip");
        } else if (property.equals("MayPlaySource")) {
            StaticAbility m = sa.getMayPlay();
            if (m == null) {
                return false;
            }
            return source.equals(m.getHostCard());
        } else if (property.startsWith("IsTargeting")) {
            String[] k = property.split(" ", 2);
            boolean found = false;
            for (GameObject o : AbilityUtils.getDefinedObjects(source, k[1], spellAbility)) {
                if (sa.isTargeting(o)) {
                    found = true;
                    break;
                }
            }
            return found;
        } else if (property.equals("YouCtrl")) {
            return sa.getActivatingPlayer().equals(sourceController);
        } else if (property.equals("OppCtrl")) {
            return sa.getActivatingPlayer().isOpponentOf(sourceController);
        } else if (sa.getHostCard() != null) {
            return sa.getHostCard().hasProperty(property, sourceController, source, spellAbility);
        }

        return true;
    }
}
