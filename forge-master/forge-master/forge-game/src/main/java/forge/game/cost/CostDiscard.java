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
package forge.game.cost;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

/**
 * The Class CostDiscard.
 */
public class CostDiscard extends CostPartWithList {
    // Discard<Num/Type{/TypeDescription}>

    // Inputs

    protected boolean firstTime = false;

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost discard.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostDiscard(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    public int paymentOrder() { return 10; }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Discard ");

        final Integer i = this.convertAmount();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        }
        else if (this.getType().equals("Hand")) {
            sb.append("your hand");
        }
        else if (this.getType().equals("LastDrawn")) {
            sb.append("the last card you drew this turn");
        }
        else {
            final StringBuilder desc = new StringBuilder();

            if (this.getType().equals("Card") || this.getType().equals("Random")) {
                desc.append("card");
            }
            else {
                desc.append(this.getTypeDescription() == null ? this.getType() : this.getTypeDescription()).append(" card");
            }

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc.toString()));

            if (this.getType().equals("Random")) {
                sb.append(" at random");
            }
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        final Card source = ability.getHostCard();

        CardCollectionView handList = payer.canDiscardBy(ability) ? payer.getCardsIn(ZoneType.Hand) : CardCollection.EMPTY;
        String type = this.getType();
        final Integer amount = this.convertAmount();

        if (this.payCostFromSource()) {
            return source.canBeDiscardedBy(ability);
        }
        else {
            if (type.equals("Hand")) {
                return payer.canDiscardBy(ability);
                // this will always work
            }
            else if (type.equals("LastDrawn")) {
                final Card c = payer.getLastDrawnCard();
                return handList.contains(c);
            }
            else {
                boolean sameName = false;
                if (type.contains("+WithSameName")) {
                    sameName = true;
                    type = TextUtil.fastReplace(type, "+WithSameName", "");
                }
                if (!type.equals("Random") && !type.contains("X")) {
                    // Knollspine Invocation fails to activate without the above conditional
                    handList = CardLists.getValidCards(handList, type.split(";"), payer, source, ability);
                }
                if (sameName) {
                    for (Card c : handList) {
                        if (CardLists.filter(handList, CardPredicates.nameEquals(c.getName())).size() > 1) {
                            return true;
                        }
                    }
                    return false;
                }
                int adjustment = 0;
                if (source.isInZone(ZoneType.Hand) && payer.equals(source.getOwner())) {
                    // If this card is in my hand, I can't use it to pay for it's own cost
                    if (handList.contains(source)) {
                        adjustment = 1;
                    }
                }

                if ((amount != null) && (amount > handList.size() - adjustment)) {
                    // not enough cards in hand to pay
                    return false;
                }
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        return targetCard.getController().discard(targetCard, null, null);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "Discarded";
    }
    public String getHashForCardList() {
    	return "DiscardedCards";
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    protected void handleBeforePayment(Player ai, SpellAbility ability, CardCollectionView targetCards) {
        firstTime = ai.getNumDiscardedThisTurn() == 0;
    }

    @Override
    protected void handleChangeZoneTrigger(Player payer, SpellAbility ability, CardCollectionView targetCards) {
        super.handleChangeZoneTrigger(payer, ability, targetCards);

        if (!targetCards.isEmpty())
        {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Player, payer);
            runParams.put(AbilityKey.Cards, new CardCollection(targetCards));
            runParams.put(AbilityKey.Cause, ability);
            runParams.put(AbilityKey.FirstTime, firstTime);
            payer.getGame().getTriggerHandler().runTrigger(TriggerType.DiscardedAll, runParams, false);
        }
    }
}
