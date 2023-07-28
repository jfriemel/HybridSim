package com.github.jfriemel.hybridsim.cli

import com.github.jfriemel.hybridsim.system.Configuration

object FullSequentialScheduler {

    /**
     * Sequentially actives robots (every robot exactly once per round) until all robots are finished or all target
     * nodes are occupied by tiles.
     *
     * @return The number of rounds until termination.
     */
    fun run(): Int {
        var round = 0
        var finished: Boolean
        val remainingTarget = Configuration.targetNodes.minus(Configuration.tiles.keys).toMutableSet()
        do {
            finished = true
            val robots = Configuration.robots.values.shuffled()
            for (robot in robots) {
                robot.triggerActivate(withUndo = false)
                finished = finished && robot.finished()
            }
            round += 1
            if (Configuration.targetNodes.isNotEmpty()) {
                remainingTarget.removeAll(Configuration.tiles.keys)
                if (remainingTarget.isEmpty()) {
                    finished = true
                }
            }
        } while (!finished)
        return round
    }

}
