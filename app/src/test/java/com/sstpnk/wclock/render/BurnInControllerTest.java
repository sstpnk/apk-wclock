package com.sstpnk.wclock.render;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BurnInControllerTest {
    @Test
    public void returnsSafeZoneWithinScreenBounds() {
        BurnInController controller = new BurnInController(6);
        BurnInController.Zone zone = controller.zoneForIndex(2, 1920, 1080, 500, 220);
        assertTrue(zone.left >= 0);
        assertTrue(zone.top >= 0);
        assertTrue(zone.left + 500 <= 1920);
        assertTrue(zone.top + 220 <= 1080);
    }
}
