package hybridsim

import kotlinx.coroutines.delay
import kotlin.random.Random

object Scheduler {

    private var active = false
    private var intervalTime = 500L

    fun start() {
        active = true
    }

    fun stop() {
        active = false
    }

    fun toggle() {
        when (active) {
            true -> stop()
            false -> start()
        }
    }

    fun setIntervalTime(intervalTime: Long) {
        if (intervalTime > 0L) {
            this.intervalTime = intervalTime
        } else {
            this.intervalTime = 1L
        }
    }

    suspend fun run() {
        while (true) {
            // Pick a random robot to activate (fair sequential scheduler)
            val robots = Configuration.robots.values.toList()
            val robotIndex = Random.nextInt(robots.size)
            robots[robotIndex].activate()

            // Sleep for a short period
            delay(intervalTime)

            // If the scheduler has been deactivated, wait until it is activated again
            while (!active) {
                delay(10L)
            }
        }
    }

}
