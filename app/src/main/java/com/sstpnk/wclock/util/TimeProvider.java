package com.sstpnk.wclock.util;

public interface TimeProvider {
    long nowMillis();

    final class SystemTimeProvider implements TimeProvider {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    }
}
