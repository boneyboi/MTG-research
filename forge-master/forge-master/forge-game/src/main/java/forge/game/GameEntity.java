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
package forge.game;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardDamageMap;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.event.GameEventCardAttachment;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public abstract class GameEntity extends GameObject implements IIdentifiable {
    protected final int id;
    private String name = "";
    private int preventNextDamage = 0;
    protected CardCollection attachedCards;
    private Map<Card, Map<String, String>> preventionShieldsWithEffects = Maps.newTreeMap();
    protected Map<CounterType, Integer> counters = Maps.newEnumMap(CounterType.class);

    protected GameEntity(int id0) {
        id = id0;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public void setName(final String s) {
        name = s;
        getView().updateName(this);
    }

    public final int addDamage(final int damage, final Card source, boolean isCombat, boolean noPrevention,
            final CardDamageMap damageMap, final CardDamageMap preventMap, GameEntityCounterTable counterTable, final SpellAbility cause) {
        if (noPrevention) {
            return addDamageWithoutPrevention(damage, source, damageMap, preventMap, counterTable, cause);
        } else if (isCombat) {
            return addCombatDamage(damage, source, damageMap, preventMap, counterTable);
        } else {
            return addDamage(damage, source, damageMap, preventMap, counterTable, cause);
        }
    }

    public int addDamage(final int damage, final Card source, final CardDamageMap damageMap,
            final CardDamageMap preventMap, GameEntityCounterTable counterTable, final SpellAbility cause) {
        int damageToDo = damage;

        damageToDo = replaceDamage(damageToDo, source, false, true, damageMap, preventMap, counterTable, cause);
        damageToDo = preventDamage(damageToDo, source, false, preventMap, cause);

        return addDamageAfterPrevention(damageToDo, source, false, damageMap, counterTable);
    }

    public final int addCombatDamage(final int damage, final Card source, final CardDamageMap damageMap,
            final CardDamageMap preventMap, GameEntityCounterTable counterTable) {
        int damageToDo = damage;

        damageToDo = replaceDamage(damageToDo, source, true, true, damageMap, preventMap, counterTable, null);
        damageToDo = preventDamage(damageToDo, source, true, preventMap, null);

        if (damageToDo > 0) {
            source.getDamageHistory().registerCombatDamage(this);
        }
        // damage prevention is already checked
        return addCombatDamageBase(damageToDo, source, damageMap, counterTable);
    }

    protected int addCombatDamageBase(final int damage, final Card source, CardDamageMap damageMap, GameEntityCounterTable counterTable) {
        return addDamageAfterPrevention(damage, source, true, damageMap, counterTable);
    }

    public int addDamageWithoutPrevention(final int damage, final Card source, final CardDamageMap damageMap,
            final CardDamageMap preventMap, GameEntityCounterTable counterTable, final SpellAbility cause) {
        int damageToDo = replaceDamage(damage, source, false, false, damageMap, preventMap, counterTable, cause);
        return addDamageAfterPrevention(damageToDo, source, false, damageMap, counterTable);
    }

    public int replaceDamage(final int damage, final Card source, final boolean isCombat, final boolean prevention,
            final CardDamageMap damageMap, final CardDamageMap preventMap, GameEntityCounterTable counterTable, final SpellAbility cause) {
        // Replacement effects
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.DamageSource, source);
        repParams.put(AbilityKey.DamageAmount, damage);
        repParams.put(AbilityKey.IsCombat, isCombat);
        repParams.put(AbilityKey.NoPreventDamage, !prevention);
        repParams.put(AbilityKey.DamageMap, damageMap);
        repParams.put(AbilityKey.PreventMap, preventMap);
        repParams.put(AbilityKey.CounterTable, counterTable);
        if (cause != null) {
            repParams.put(AbilityKey.Cause, cause);
        }

        switch (getGame().getReplacementHandler().run(ReplacementType.DamageDone, repParams)) {
        case NotReplaced:
            return damage;
        case Updated:
            int newDamage = (int) repParams.get(AbilityKey.DamageAmount);
            GameEntity newTarget = (GameEntity) repParams.get(AbilityKey.Affected);
            // check if this is still the affected card or player
            if (this.equals(newTarget)) {
                return newDamage;
            } else {
                if (prevention) {
                    newDamage = newTarget.preventDamage(newDamage, source, isCombat, preventMap, cause);
                }
                newTarget.addDamageAfterPrevention(newDamage, source, isCombat, damageMap, counterTable);
            }
        default:
            return 0;
        }
    }

    // This function handles damage after replacement and prevention effects are applied
    public abstract int addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat, CardDamageMap damageMap, GameEntityCounterTable counterTable);

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    public abstract int staticDamagePrevention(final int damage, final Card source, final boolean isCombat, final boolean isTest);

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    public abstract int staticReplaceDamage(final int damage, final Card source, final boolean isCombat);

    public final int preventDamage(
            final int damage, final Card source, final boolean isCombat, CardDamageMap preventMap,
            final SpellAbility cause) {
        if (!source.canDamagePrevented(isCombat)) {
            return damage;
        }

        int restDamage = damage;

        // first try to replace the damage
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.DamageSource, source);
        repParams.put(AbilityKey.DamageAmount, damage);
        repParams.put(AbilityKey.IsCombat, isCombat);
        repParams.put(AbilityKey.Prevention, true);
        repParams.put(AbilityKey.PreventMap, preventMap);
        if (cause != null) {
            repParams.put(AbilityKey.Cause, cause);
        }

        switch (getGame().getReplacementHandler().run(ReplacementType.DamageDone, repParams)) {
        case NotReplaced:
            restDamage = damage;
            break;
        case Updated:
            restDamage = (int) repParams.get(AbilityKey.DamageAmount);
            break;
        default:
            restDamage = 0;
        }

        // then apply static Damage Prevention effects
        restDamage = staticDamagePrevention(restDamage, source, isCombat, false);

        // then apply ShieldEffects with Special Effect
        restDamage = preventShieldEffect(restDamage);

        // then do Shield with only number
        if (restDamage <= 0) {
            restDamage = 0;
        } else if (restDamage >= getPreventNextDamage()) {
            restDamage = restDamage - getPreventNextDamage();
            setPreventNextDamage(0);
        } else {
            setPreventNextDamage(getPreventNextDamage() - restDamage);
            restDamage = 0;
        }

        // if damage is greater than restDamage, damage was prevented
        if (damage > restDamage) {
            int prevent = damage - restDamage;
            preventMap.put(source, this, damage - restDamage);

            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.DamageTarget, this);
            runParams.put(AbilityKey.DamageAmount, prevent);
            runParams.put(AbilityKey.DamageSource, source);
            runParams.put(AbilityKey.IsCombatDamage, isCombat);

            getGame().getTriggerHandler().runTrigger(TriggerType.DamagePrevented, runParams, false);
        }

        return restDamage;
    }

    protected abstract int preventShieldEffect(final int damage);

    public int getPreventNextDamage() {
        return preventNextDamage;
    }
    public void setPreventNextDamage(final int n) {
        preventNextDamage = n;
    }
    public void addPreventNextDamage(final int n) {
        preventNextDamage += n;
    }
    public void subtractPreventNextDamage(final int n) {
        preventNextDamage -= n;
    }
    public void resetPreventNextDamage() {
        preventNextDamage = 0;
    }

    // PreventNextDamageWithEffect
    public Map<Card, Map<String, String>> getPreventNextDamageWithEffect() {
        return preventionShieldsWithEffects;
    }
    public int getPreventNextDamageTotalShields() {
        int shields = preventNextDamage;
        for (final Map<String, String> value : preventionShieldsWithEffects.values()) {
            shields += Integer.valueOf(value.get("ShieldAmount"));
        }
        return shields;
    }
    /**
     * Adds a damage prevention shield with an effect that happens at time of prevention.
     * @param shieldSource - The source card which generated the shield
     * @param effectMap - A map of the effect occurring with the damage prevention
     */
    public void addPreventNextDamageWithEffect(final Card shieldSource, Map<String, String> effectMap) {
        if (preventionShieldsWithEffects.containsKey(shieldSource)) {
            int currentShields = Integer.valueOf(preventionShieldsWithEffects.get(shieldSource).get("ShieldAmount"));
            currentShields += Integer.valueOf(effectMap.get("ShieldAmount"));
            effectMap.put("ShieldAmount", Integer.toString(currentShields));
            preventionShieldsWithEffects.put(shieldSource, effectMap);
        } else {
            preventionShieldsWithEffects.put(shieldSource, effectMap);
        }
    }
    public void subtractPreventNextDamageWithEffect(final Card shieldSource, final int n) {
        int currentShields = Integer.valueOf(preventionShieldsWithEffects.get(shieldSource).get("ShieldAmount"));
        if (currentShields > n) {
            preventionShieldsWithEffects.get(shieldSource).put("ShieldAmount", String.valueOf(currentShields - n));
        } else {
            preventionShieldsWithEffects.remove(shieldSource);
        }
    }
    public void resetPreventNextDamageWithEffect() {
        preventionShieldsWithEffects.clear();
    }

    public abstract boolean hasKeyword(final String keyword);
    public abstract boolean hasKeyword(final Keyword keyword);

    public final CardCollectionView getEnchantedBy() {
        if (attachedCards == null) {
            return CardCollection.EMPTY;
        }
        // enchanted means attached by Aura
        return CardLists.filter(attachedCards, CardPredicates.Presets.AURA);
    }

    public final CardCollectionView getAttachedCards() {
        return CardCollection.getView(attachedCards);
    }

    public final void setAttachedCards(final Iterable<Card> cards) {
        if (cards == null) {
            attachedCards = null;
        }
        else {
            attachedCards = new CardCollection(cards);
        }

        getView().updateAttachedCards(this);
    }

    public final boolean hasCardAttachments() {
        return FCollection.hasElements(attachedCards);
    }

    public final boolean isEnchanted() {
        if (attachedCards == null) {
            return false;
        }

        // enchanted means attached by Aura
        return CardLists.count(attachedCards, CardPredicates.Presets.AURA) > 0;
    }

    public final boolean hasCardAttachment(Card c) {
        return FCollection.hasElement(attachedCards, c);
    }
    public final boolean isEnchantedBy(Card c) {
        // Rule 303.4k  Even if c is no Aura it still counts
        return hasCardAttachment(c);
    }

    public final boolean hasCardAttachment(final String cardName) {
        if (attachedCards == null) {
            return false;
        }
        return CardLists.count(getAttachedCards(), CardPredicates.nameEquals(cardName)) > 0;
    }
    public final boolean isEnchantedBy(final String cardName) {
        // Rule 303.4k  Even if c is no Aura it still counts
        return hasCardAttachment(cardName);
    }

    /**
     * internal method
     * @param Card c
     */
    public final void addAttachedCard(final Card c) {
        if (attachedCards == null) {
            attachedCards = new CardCollection();
        }

        if (attachedCards.add(c)) {
            getView().updateAttachedCards(this);
            getGame().fireEvent(new GameEventCardAttachment(c, null, this));
        }
    }

    /**
     * internal method
     * @param Card c
     */
    public final void removeAttachedCard(final Card c) {
        if (attachedCards == null) { return; }

        if (attachedCards.remove(c)) {
            if (attachedCards.isEmpty()) {
                attachedCards = null;
            }
            getView().updateAttachedCards(this);
            getGame().fireEvent(new GameEventCardAttachment(c, this, null));
        }
    }

    public final void unAttachAllCards() {
        for (Card c : Lists.newArrayList(getAttachedCards())) {
            c.unattachFromEntity(this);
        }
    }

    public boolean canBeAttached(final Card attach) {
        return canBeAttached(attach, false);
    }

    public boolean canBeAttached(final Card attach, boolean checkSBA) {
        // master mode
        if (!attach.isAttachment() || attach.isCreature() || equals(attach)) {
            return false;
        }

        // CantTarget static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.applyAbility("CantAttach", attach, this)) {
                    return false;
                }
            }
        }

        if (attach.isAura() && !canBeEnchantedBy(attach)) {
            return false;
        }
        if (attach.isEquipment() && !canBeEquippedBy(attach)) {
            return false;
        }
        if (attach.isFortification() && !canBeFortifiedBy(attach)) {
            return false;
        }

        // true for all
        return !hasProtectionFrom(attach, checkSBA);
    }

    protected boolean canBeEquippedBy(final Card aura) {
        /**
         * Equip only to Creatures which are cards
         */
        return false;
    }

    protected boolean canBeFortifiedBy(final Card aura) {
        /**
         * Equip only to Lands which are cards
         */
        return false;
    }

    protected boolean canBeEnchantedBy(final Card aura) {

        SpellAbility sa = aura.getFirstAttachSpell();
        TargetRestrictions tgt = null;
        if (sa != null) {
            tgt = sa.getTargetRestrictions();
        }

        return !((tgt != null) && !isValid(tgt.getValidTgts(), aura.getController(), aura, sa));
    }

    public boolean hasProtectionFrom(final Card source) {
        return hasProtectionFrom(source, false);
    }

    public abstract boolean hasProtectionFrom(final Card source, final boolean checkSBA);

    // Counters!
    public boolean hasCounters() {
        return !counters.isEmpty();
    }

    // get all counters from a card
    public final Map<CounterType, Integer> getCounters() {
        return counters;
    }

    public final int getCounters(final CounterType counterName) {
        Integer value = counters.get(counterName);
        return value == null ? 0 : value;
    }

    public void setCounters(final CounterType counterType, final Integer num) {
        if (num <= 0) {
            counters.remove(counterType);
        } else {
            counters.put(counterType, num);
        }
    }

    abstract public void setCounters(final Map<CounterType, Integer> allCounters);

    abstract public boolean canReceiveCounters(final CounterType type);
    abstract public int addCounter(final CounterType counterType, final int n, final Player source, final boolean applyMultiplier, final boolean fireEvents, GameEntityCounterTable table);
    abstract public void subtractCounter(final CounterType counterName, final int n);
    abstract public void clearCounters();


    @Override
    public final boolean equals(Object o) {
        if (o == null) { return false; }
        return o.hashCode() == id && o.getClass().equals(getClass());
    }

    @Override
    public final int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract Game getGame();
    public abstract GameEntityView getView();
}
