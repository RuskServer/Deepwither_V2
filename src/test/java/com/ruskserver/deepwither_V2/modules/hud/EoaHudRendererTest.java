package com.ruskserver.deepwither_V2.modules.hud;

import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.RenderedSkillBar;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SkillBarSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SkillSlotSnapshot;
import com.ruskserver.deepwither_V2.modules.hud.EoaSkillBarLayout.SlotState;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EoaHudRendererTest {

    @Test
    void totalAdvanceAlwaysMatchesHotbarWidthAcrossStates() {
        List<EoaHudLayout.HudSnapshot> cases = List.of(
                EoaHudLayout.HudSnapshot.of(1240, 1500, 92, 120, 0, 0, 0),
                EoaHudLayout.HudSnapshot.of(1240, 1500, 92, 120, 0, 480, 600),
                EoaHudLayout.HudSnapshot.of(320, 1500, 64, 120, 0, 600, 600),
                EoaHudLayout.HudSnapshot.of(260, 1500, 18, 120, 320, 0, 0),
                EoaHudLayout.HudSnapshot.of(0, 1500, 0, 120, 0, 0, 0),
                EoaHudLayout.HudSnapshot.of(1500, 1500, 120, 120, 1500, 600, 600)
        );

        for (EoaHudLayout.HudSnapshot snapshot : cases) {
            EoaHudLayout.RenderedLayers layers = EoaHudLayout.build(snapshot);
            assertFalse(layers.barText().isEmpty());
            assertFalse(layers.numText().isEmpty());

            int totalAdvance = EoaHudLayout.measureAdvance(layers.barText())
                    + EoaHudLayout.measureAdvance(layers.numText());
            assertEquals(EoaHudLayout.TOTAL_WIDTH, totalAdvance,
                    "Total advance must equal 182 GUI px for snapshot: " + snapshot);
        }
    }

    @Test
    void skillBarTotalAdvanceAlwaysMatchesHotbarWidth() {
        SkillBarSnapshot bar = new SkillBarSnapshot(List.of(
                new SkillSlotSnapshot("fireball", SlotState.READY, 0, ""),
                SkillSlotSnapshot.cooldown("blizzard", 3400L, 8000L),
                SkillSlotSnapshot.cooldown("chain_lightning", 12000L, 15000L),
                new SkillSlotSnapshot("holy_nova", SlotState.CASTING, 0, ""),
                new SkillSlotSnapshot("flame_pillar", SlotState.NO_MANA, 0, ""),
                new SkillSlotSnapshot("blink", SlotState.READY, 0, ""),
                new SkillSlotSnapshot("divine_shield", SlotState.READY, 0, ""),
                SkillSlotSnapshot.locked("rain_of_arrows"),
                SkillSlotSnapshot.empty()
        ));

        RenderedSkillBar rendered = EoaSkillBarLayout.build(bar);
        assertFalse(rendered.slotText().isEmpty());
        assertFalse(rendered.cdText().isEmpty());

        int totalAdvance = EoaSkillBarLayout.measureAdvance(rendered.slotText())
                + EoaSkillBarLayout.measureAdvance(rendered.cdText());
        assertEquals(EoaHudLayout.TOTAL_WIDTH, totalAdvance);
    }

    @Test
    void all48SkillsHaveUniqueIconGlyphs() {
        assertEquals(48, EoaSkillBarLayout.SKILL_ICON_ORDER.size());
        Set<Character> glyphs = new HashSet<>();
        for (String skillId : EoaSkillBarLayout.SKILL_ICON_ORDER) {
            glyphs.add(EoaSkillBarLayout.iconGlyphFor(skillId));
        }
        assertEquals(48, glyphs.size());
    }

    @Test
    void spaceEncodingMatchesExactSignedDelta() {
        for (int delta = -260; delta <= 260; delta += 13) {
            StringBuilder sb = new StringBuilder();
            EoaHudLayout.appendSpace(sb, delta);
            assertEquals(delta, EoaHudLayout.measureAdvance(sb.toString()), "Delta mismatch for " + delta);
        }
        assertEquals(0, EoaHudLayout.measureAdvance(""));
    }
}
