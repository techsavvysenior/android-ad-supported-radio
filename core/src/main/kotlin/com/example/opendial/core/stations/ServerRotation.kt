package com.example.opendial.core.stations

import kotlin.random.Random

/**
 * The directory runs on several mirrors and asks clients to find them through DNS
 * (`all.api.radio-browser.info`), pick one at random and fall back to the next on failure, rather
 * than hard-coding a server. This takes the host names the DNS lookup returned and gives the order
 * to try them in.
 */
object ServerRotation {
    private const val SUFFIX = ".api.radio-browser.info"

    fun order(hosts: Collection<String>, random: Random): List<String> =
        hosts.map { it.trim().trimEnd('.').lowercase() }
            .filter { it.endsWith(SUFFIX) && it != "all$SUFFIX" }
            .distinct()
            .sorted()
            .shuffled(random)
}
