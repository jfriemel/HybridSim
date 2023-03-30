package com.github.jfriemel.hybridsim.ui

import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.Scheduler
import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import java.io.File
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

object AlgorithmLoader {

    private var invocator: Invocable? = null

    /**
     * Loads a new algorithm from the [scriptFile] (kts script, see examples). The script implements a [Robot] and
     * overrides the existing activate() function.
     *
     * Important: The script needs to have a getRobot() function of the following form:
     *     fun getRobot(orientation: Int, node: Node): Robot
     *
     * If this function is absent, or there is a syntax error in the script or the script could not be loaded for any
     * other reason, the program crashes.
     */
    fun loadAlgorithm(scriptFile: File) {
        Scheduler.stop()  // Make sure the scheduler is not running while the robots are replaced
        val engine = ScriptEngineManager().getEngineByExtension("kts") as Compilable
        engine.compile("import com.github.jfriemel.hybridsim.entities.*; import com.badlogic.gdx.graphics.Color").eval()
        engine.compile(scriptFile.readText().trim()).eval()
        invocator = engine as Invocable
        Configuration.robots.keys.forEach { replaceRobot(it) }
        Configuration.clearUndoQueues()
    }

    /**
     * Replaces the [Robot] at the given [node] with the robot from the most recently loaded kts script.
     * Does nothing if there is no robot at the [node] or if no kts script was loaded before the call.
     */
    fun replaceRobot(node: Node) {
        val robot = Configuration.robots[node] ?: return
        Configuration.robots[node] = getAlgorithmRobot(robot)
    }

    /** Returns a [Robot] with the most recently loaded algorithm and the same properties as the given [robot]. */
    fun getAlgorithmRobot(robot: Robot): Robot {
        return invocator?.let { inv ->
            inv.invokeFunction("getRobot", robot.node, robot.orientation) as Robot
        } ?: robot
    }
}
