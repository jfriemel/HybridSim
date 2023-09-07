package com.github.jfriemel.hybridsim.cli

import com.github.jfriemel.hybridsim.system.Commons
import com.github.jfriemel.hybridsim.system.Configuration
import kotlin.math.max

object FullSequentialScheduler {

    /**
     * Sequentially actives robots (every robot exactly once per round) until all robots are
     * finished or all target nodes are occupied by tiles or the number of rounds reaches [limit].
     *
     * @return The number of rounds (rounded to the nearest multiple of the number of tiles) until
     *   termination or null if the number of rounds reaches [limit].
     */
    fun run(limit: Int): Int? {
        var rounds = 0
        var finished = false
        val remainingTarget =
            if (Configuration.targetNodes.isEmpty()) {
                null
            } else {
                Configuration.targetNodes.minus(Configuration.tiles.keys).toMutableSet()
            }

        val numTiles = max(Configuration.tiles.keys.size, 1)
        while (!finished) {
            repeat(numTiles) {
                finished = true
                val robots = Configuration.robots.values.shuffled(Commons.random)
                for (robot in robots) {
                    robot.triggerActivate(withUndo = false)
                    finished = finished && robot.finished()
                }
            }

            if (remainingTarget != null) {
                remainingTarget.removeAll(Configuration.tiles.keys)
                if (remainingTarget.isEmpty()) {
                    finished = true
                }
            }

            rounds += numTiles
            if (rounds >= limit) {
                return null
            }
        }

        return rounds
    }
}
