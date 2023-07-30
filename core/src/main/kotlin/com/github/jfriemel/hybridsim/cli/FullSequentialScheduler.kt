package com.github.jfriemel.hybridsim.cli

import com.github.jfriemel.hybridsim.system.Configuration

object FullSequentialScheduler {

    /**
     * Sequentially actives robots (every robot exactly once per round) until all robots are finished or all target
     * nodes are occupied by tiles or the number of rounds reaches [threshold].
     *
     * @return The number of rounds until termination or null if the number of rounds reaches [threshold].
     */
    fun run(threshold: Int): Int? {
        var rounds = 0
        var finished: Boolean
        val remainingTarget = Configuration.targetNodes.minus(Configuration.tiles.keys).toMutableSet()
        do {
            finished = true
            val robots = Configuration.robots.values.shuffled()
            for (robot in robots) {
                robot.triggerActivate(withUndo = false)
                finished = finished && robot.finished()
            }

            if (Configuration.targetNodes.isNotEmpty()) {
                remainingTarget.removeAll(Configuration.tiles.keys)
                if (remainingTarget.isEmpty()) {
                    finished = true
                }
            }

            if (rounds++ == threshold) {
                return null
            }
        } while (!finished)
        return rounds
    }

}
