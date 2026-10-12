package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

/**
 * Past this, a position is old enough that both clients say so in grey.
 *
 * The promise the grey makes is "this device has stopped saying where it is",
 * so it has to be longer than the quietest way of collecting can go without a
 * word — see [CollectionMode], whose slowest round of asking and waiting comes
 * to twenty-five minutes — and short enough that a device which really has
 * stopped is noticed within the hour. The web page keeps the same half hour.
 */
val STALE_AFTER: Duration = Duration.ofMinutes(30)
