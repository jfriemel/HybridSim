package com.github.jfriemel.hybridsim.system

import kotlinx.coroutines.delay
import kotlin.random.Random
import ktx.log.logger

private val logger = logger<Scheduler>()

object Scheduler {

    private var active = false
    private var cycleDelay = 100L  // Delay after completion of each activation cycle in ms
    private var activationsPerCycle = 1  // Number of activations per scheduler cycle

    /** Returns true if the scheduler is running. */
    @Suppress("Unused")
    fun isRunning(): Boolean {
        return active
    }

    /** Starts the scheduler. */
    @SuppressWarnings("WeakerAccess")
    fun start() {
        if (!active) {
            logger.debug { "Scheduler started" }
        }
        active = true
    }

    /** Stops the scheduler. */
    fun stop() {
        if (active) {
            logger.debug { "Scheduler stopped" }
        }
        active = false
    }

    /** Starts the scheduler if it is idle or stops the scheduler if it is running. */
    fun toggle() {
        if (active) {
            stop()
        } else {
            start()
        }
    }

    /** Sets the (expected) interval time between robot activations to [intervalTime] * 0.1ms. */
    fun setIntervalTime(intervalTime: Long) {
        if (intervalTime > 0L) {
            if (intervalTime < 10L) {
                cycleDelay = intervalTime
                activationsPerCycle = 10
            } else {
                cycleDelay = intervalTime / 10L
                activationsPerCycle = 1
            }
        } else {
            cycleDelay = 1L
        }
    }

    /** Returns the (expected) interval time between robot activations in 0.1ms. */
    fun getIntervalTime(): Long {
        return cycleDelay * 10 / activationsPerCycle
    }

    /** Infinite loop running in a separate coroutine, performs the actual scheduling. */
    suspend fun run() {
        while (true) {
            // If the scheduler has been deactivated, wait until it is activated again
            while (!active) {
                delay(10L)
            }

            val robots = Configuration.robots.values.toList()
            if (robots.isEmpty()) {
                logger.debug { "No robots in configuration" }
                stop()
                continue
            }

            // Activate a number of robots (sequential activations, no overlap)
            repeat(activationsPerCycle) {
                // Pick a random robot to activate (fair sequential scheduler)
                robots[Random.nextInt(robots.size)].activate()
            }

            // Sleep for a short period between cycles
            delay(cycleDelay)
        }
    }

}
