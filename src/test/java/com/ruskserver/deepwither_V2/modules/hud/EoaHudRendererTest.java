package com.ruskserver.deepwither_V2.modules.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

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
    void spaceEncodingMatchesExactSignedDelta() {
        for (int delta = -260; delta <= 260; delta += 13) {
            StringBuilder sb = new StringBuilder();
            EoaHudLayout.appendSpace(sb, delta);
            assertEquals(delta, EoaHudLayout.measureAdvance(sb.toString()), "Delta mismatch for " + delta);
        }
        assertEquals(0, EoaHudLayout.measureAdvance(""));
    }
}
