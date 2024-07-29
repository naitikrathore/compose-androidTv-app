package com.mediatek.dtv.tvinput.atsc3tuner.common;

import android.os.Bundle;

interface IBroadcastClockService {
    /**
     * Gets UTC time from BroadcastClock.<br>
     * This is a synchronous function.
     *
     * <p>Pre-condition
     * <p>- Unconditional.
     *
     * <p>Post-condition
     * <p>- Unconditional.
     * <br>
     * @return - The difference, measured in milliseconds, between the current time and midnight,
     *           January 1, 1970 UTC.<br>
     *         - {@linkplain Constants#ERROR_NOT_OBTAINED} if UTC time hasn't been obtained yet.
     * @throws UnsupportedOperationException If TIS doesn't support.
     */
    long getUtcTime();

    /**
     * Gets local time from BroadcastClock.<br>
     * This is a synchronous function.
     *
     * <p>Pre-condition
     * <p>- Unconditional.
     *
     * <p>Post-condition
     * <p>- Unconditional.
     * <br>
     * @return - Sum of the offset time, measured in milliseconds, from UTC at a time when current
     *           UTC time and the difference, measured in milliseconds, between the current time and
     *           midnight, January 1, 1970 UTC.<br>
     *         - {@linkplain Constants#ERROR_NOT_OBTAINED} if UTC time or the information of
     *           time zone hasn't been obtained yet.
     * @throws UnsupportedOperationException If TIS doesn't support.
     */
    long getLocalTime();

    /**
     * Gets the information of time zone from BroadcastClock.<br>
     * This is a synchronous function.
     *
     * <p>Pre-condition
     * <p>- Unconditional.
     *
     * <p>Post-condition
     * <p>- Unconditional.
     * <br>
     * @return - Bundle containing the following keys.<br>
     *             1. {@linkplain Constants#KEY_LOCAL_TIME_OFFSET}<br>
     *             2. {@linkplain Constants#KEY_TIME_OF_CHANGE}<br>
     *             3. {@linkplain Constants#KEY_NEXT_TIME_OFFSET}
     * @throws UnsupportedOperationException If TIS doesn't support.
     */
    Bundle getTimeZoneInfo();
}
