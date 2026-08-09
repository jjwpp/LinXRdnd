package com.jjwpp.linxr.dm.tool;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 骰子工具 — 所有掷骰的唯一入口。
 * <p>
 * 规则：所有随机数生成必须通过此类，禁止在其他地方直接调用 ThreadLocalRandom。
 * Tool 返回的结果具有最高优先级，AI 不得覆盖。
 */
@Component
public class DiceTool {

    /**
     * 掷一个 d20
     */
    public int rollD20() {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }

    /**
     * 掷指定面数的骰子
     */
    public int rollDice(int sides) {
        return ThreadLocalRandom.current().nextInt(1, sides + 1);
    }

    /**
     * 掷多个骰子并求和
     *
     * @param count 骰子数量
     * @param sides 骰子面数
     * @return 总和
     */
    public int rollDice(int count, int sides) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += rollDice(sides);
        }
        return total;
    }

    /**
     * 解析并投掷骰子公式。
     * 支持格式：
     *   "1d6"     → 1个d6
     *   "2d8+3"   → 2个d8 + 3
     *   "3d4-1"   → 3个d4 - 1
     *   "0"       → 0（无伤害）
     *   "5"       → 5（固定值）
     *
     * @param formula 骰子公式字符串
     * @return 投掷结果
     */
    public int rollFormula(String formula) {
        if (formula == null || formula.isBlank()) return 0;
        formula = formula.trim();

        // 纯数字
        if (formula.matches("-?\\d+")) {
            return Integer.parseInt(formula);
        }

        // 解析 NdM[+/-K]
        int plusIdx = formula.lastIndexOf('+');
        int minusIdx = formula.lastIndexOf('-');
        int modifier = 0;
        String dicePart = formula;

        if (plusIdx > 0 && !formula.startsWith("-")) {
            dicePart = formula.substring(0, plusIdx).trim();
            modifier = Integer.parseInt(formula.substring(plusIdx + 1).trim());
        } else if (minusIdx > 0) {
            dicePart = formula.substring(0, minusIdx).trim();
            modifier = -Integer.parseInt(formula.substring(minusIdx + 1).trim());
        }

        // 解析 NdM
        if (dicePart.contains("d") || dicePart.contains("D")) {
            String[] parts = dicePart.split("[dD]");
            int count = parts[0].isEmpty() ? 1 : Integer.parseInt(parts[0].trim());
            int sides = Integer.parseInt(parts[1].trim());
            return rollDice(count, sides) + modifier;
        }

        return modifier;
    }

    /**
     * 掷 d20 并返回详细结果（含暴击/失误判断）
     */
    public D20Result rollD20WithDetail() {
        int roll = rollD20();
        D20Result result = new D20Result();
        result.roll = roll;
        result.crit = (roll == 20);
        result.fumble = (roll == 1);
        return result;
    }

    public static class D20Result {
        public int roll;
        public boolean crit;    // 自然20
        public boolean fumble;  // 自然1
    }
}
