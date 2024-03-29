package com.github.jfriemel.hybridsim.system

import com.github.jfriemel.hybridsim.entities.Robot
import kotlinx.coroutines.delay
import ktx.log.logger
import java.lang.Long.max

private val logger = logger<Scheduler>()

object Scheduler {
    private var active = false
    private var cycleDelay = 120L // Delay after completion of each activation cycle in ms
    private var activationsPerCycle = 1 // Number of activations per scheduler cycle

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

    /** Sets the (expected) interval time between robot activations to [intervalTime] * 0.01ms. */
    fun setIntervalTime(intervalTime: Long) {
        val iTime = max(1L, intervalTime)
        if (iTime < 10L) {
            cycleDelay = iTime
            activationsPerCycle = 100
        } else if (iTime < 100L) {
            cycleDelay = iTime / 10L
            activationsPerCycle = 10
        } else {
            cycleDelay = iTime / 100L
            activationsPerCycle = 1
        }
    }

    /** @return The (expected) interval time between robot activations in 0.01ms. */
    fun getIntervalTime(): Long = cycleDelay * 100L / activationsPerCycle

    /** Infinite loop running in a separate coroutine, performs the actual scheduling. */
    suspend fun run() {
        schedulerLoop@ while (true) {
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
            Configuration.addUndoStep()
            for (activationIndex in 1..activationsPerCycle) {
                // Pick a random robot to activate (fair sequential scheduler)
                val robot = robots[Commons.random.nextInt(robots.size)]
                try {
                    robot.triggerActivate(withUndo = false)
                } catch (e: Exception) {
                    // Ensure Scheduler does not crash when Robot crashes due to faulty algorithm
                    // script
                    logger.error { "Robot at ${robot.node} crashed!" }
                    logger.error { e.toString() }
                    logger.error { e.stackTraceToString() }
                    stop()
                    continue@schedulerLoop
                }
            }

            try {
                // Stop when all robots are finished
                if (robots.all(Robot::finished)) {
                    stop()
                }
            } catch (e: Exception) {
                // Ensure Scheduler does not crash when Robot crashes due to faulty algorithm script
                logger.error { "Robot crashed during finished() call!" }
                logger.error { e.toString() }
                logger.error { e.stackTraceToString() }
                stop()
                continue@schedulerLoop
            }

            // Sleep for a specified period (ms) between cycles
            delay(cycleDelay)
        }
    }
}
