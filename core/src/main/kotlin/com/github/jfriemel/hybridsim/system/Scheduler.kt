package com.github.jfriemel.hybridsim.system

import kotlinx.coroutines.delay
import ktx.log.logger
import java.lang.Long.max
import kotlin.random.Random

private val logger = logger<Scheduler>()

object Scheduler {

    private var active = false
    private var cycleDelay = 100L  // Delay after completion of each activation cycle in ms
    private var activationsPerCycle = 1  // Number of activations per scheduler cycle

    /** @return True if the scheduler is running. */
    fun isRunning(): Boolean = active

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
        val iTime = max(1L, intervalTime)
        if (iTime < 10L) {
            cycleDelay = iTime
            activationsPerCycle = 10
        } else {
            cycleDelay = iTime / 10L
            activationsPerCycle = 1
        }
    }

    /** @return The (expected) interval time between robot activations in 0.1ms. */
    fun getIntervalTime(): Long = cycleDelay * 10 / activationsPerCycle

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
            run activationBlock@ {
                repeat(activationsPerCycle) {
                    // Pick a random robot to activate (fair sequential scheduler)
                    val robot = robots[Random.nextInt(robots.size)]
                    try {
                        robot.activate()
                    } catch (e: Exception) {
                        logger.error { "Robot at ${robot.node} crashed!" }
                        logger.error { e.toString() }
                        stop()
                        return@activationBlock
                    }
                }
            }

            // Stop when all robots are finished
            if (robots.all { it.finished() }) {
                stop()
            }

            // Sleep for a specified period (ms) between cycles
            delay(cycleDelay)
        }
    }

}
