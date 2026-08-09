package com.jjwpp.linxr.dm.tool;

import com.jjwpp.linxr.dm.AdventureState;
import org.springframework.stereotype.Component;

/**
 * HP管理工具 — 所有生命值变化的唯一入口。
 * <p>
 * 规则：
 * - HP不能超过 maxHp
 * - HP不能小于 0
 * - HP <= 0 时角色/敌人死亡
 * <p>
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class HpTool {

    /**
     * 对敌人施加伤害
     *
     * @return 实际造成的伤害（考虑HP下限）
     */
    public int applyDamageToEnemy(AdventureState.Enemy enemy, int damage) {
        int actualDamage = Math.min(enemy.getHp(), damage);
        enemy.setHp(Math.max(0, enemy.getHp() - damage));
        if (enemy.getHp() <= 0) {
            enemy.setAlive(false);
        }
        return actualDamage;
    }

    /**
     * 对玩家施加伤害
     *
     * @return 实际造成的伤害
     */
    public int applyDamageToPlayer(AdventureState state, int damage) {
        int actualDamage = Math.min(state.getCurrentHp(), damage);
        state.setCurrentHp(Math.max(0, state.getCurrentHp() - damage));
        return actualDamage;
    }

    /**
     * 恢复玩家HP
     *
     * @return 实际恢复的HP
     */
    public int applyHeal(AdventureState state, int heal) {
        int before = state.getCurrentHp();
        state.setCurrentHp(Math.min(state.getMaxHp(), before + heal));
        return state.getCurrentHp() - before;
    }

    /**
     * 检查角色是否死亡
     */
    public boolean isDead(int hp) {
        return hp <= 0;
    }

    /**
     * 检查所有敌人是否已死
     */
    public boolean allEnemiesDead(AdventureState.CombatState combat) {
        if (combat == null || combat.getEnemies() == null) return true;
        return combat.getEnemies().stream().noneMatch(AdventureState.Enemy::isAlive);
    }
}
