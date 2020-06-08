/**
 *
 */
package forge.game.card;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObjectPredicates;
import forge.game.ability.AbilityKey;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class CardDamageMap extends ForwardingTable<Card, GameEntity, Integer> {
    private Table<Card, GameEntity, Integer> dataMap = HashBasedTable.create();

    public CardDamageMap(Table<Card, GameEntity, Integer> damageMap) {
        this.putAll(damageMap);
    }

    public CardDamageMap() {
    }

    public void triggerPreventDamage(boolean isCombat) {
        for (Map.Entry<GameEntity, Map<Card, Integer>> e : this.columnMap().entrySet()) {
            int sum = 0;
            for (final int i : e.getValue().values()) {
                sum += i;
            }
            if (sum > 0) {
                final GameEntity ge = e.getKey();
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.DamageTarget, ge);
                runParams.put(AbilityKey.DamageAmount, sum);
                runParams.put(AbilityKey.IsCombatDamage, isCombat);

                ge.getGame().getTriggerHandler().runTrigger(TriggerType.DamagePreventedOnce, runParams, false);
            }
        }
    }

    public void triggerDamageDoneOnce(boolean isCombat, final Game game, final SpellAbility sa) {
        // Source -> Targets
        for (Map.Entry<Card, Map<GameEntity, Integer>> e : this.rowMap().entrySet()) {
            final Card sourceLKI = e.getKey();
            int sum = 0;
            for (final Integer i : e.getValue().values()) {
                sum += i;
            }
            if (sum > 0) {
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.DamageSource, sourceLKI);
                runParams.put(AbilityKey.DamageMap, Maps.newHashMap(e.getValue()));
                runParams.put(AbilityKey.IsCombatDamage, isCombat);

                game.getTriggerHandler().runTrigger(TriggerType.DamageDealtOnce, runParams, false);

                if (sourceLKI.hasKeyword(Keyword.LIFELINK)) {
                    sourceLKI.getController().gainLife(sum, sourceLKI, sa);
                }
            }
        }
        // Targets -> Source
        for (Map.Entry<GameEntity, Map<Card, Integer>> e : this.columnMap().entrySet()) {
            int sum = 0;
            for (final int i : e.getValue().values()) {
                sum += i;
            }
            if (sum > 0) {
                final GameEntity ge = e.getKey();
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.DamageTarget, ge);
                runParams.put(AbilityKey.DamageMap, Maps.newHashMap(e.getValue()));
                runParams.put(AbilityKey.IsCombatDamage, isCombat);

                game.getTriggerHandler().runTrigger(TriggerType.DamageDoneOnce, runParams, false);
            }
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.DamageMap, new CardDamageMap(this));
        runParams.put(AbilityKey.IsCombatDamage, isCombat);
        game.getTriggerHandler().runTrigger(TriggerType.DamageAll, runParams, false);
    }
    /**
     * special put logic, sum the values
     */
    @Override
    public Integer put(Card rowKey, GameEntity columnKey, Integer value) {
        Integer old = contains(rowKey, columnKey) ? get(rowKey, columnKey) : 0;
        return dataMap.put(rowKey, columnKey, value + old);
    }

    @Override
    protected Table<Card, GameEntity, Integer> delegate() {
        return dataMap;
    }

    public int filteredAmount(String validSource, String validTarget, Card host, SpellAbility sa) {
        int result = 0;

        Set<Card> filteredSource = null;
        Set<GameEntity> filteredTarget = null;
        if (validSource != null) {
            filteredSource = Sets.newHashSet(Iterables.filter(rowKeySet(), GameObjectPredicates.restriction(validSource.split(","), host.getController(), host, sa)));
        }
        if (validTarget != null) {
            filteredTarget = Sets.newHashSet(Iterables.filter(columnKeySet(), GameObjectPredicates.restriction(validTarget.split(","), host.getController(), host, sa)));
        }

        for (Table.Cell<Card, GameEntity, Integer> c : cellSet()) {
            if (filteredSource != null && !filteredSource.contains(c.getRowKey())) {
                continue;
            }
            if (filteredTarget != null && !filteredTarget.contains(c.getColumnKey())) {
                continue;
            }
            result += c.getValue();
        }

        return result;
    }

}
