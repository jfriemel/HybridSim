package com.github.jfriemel.hybridsim.system

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import ktx.log.logger
import java.io.File
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

private val logger = logger<AlgorithmLoader>()

object AlgorithmLoader {
    private var invocator: Invocable? = null

    /**
     * Loads a new algorithm from either the [scriptFile] or the [scriptString] (kts script, see
     * examples). The script implements a [Robot] and overrides the existing activate() function.
     *
     * Important: The script needs to have a getRobot() function of the following form: fun
     * getRobot(node: Node, orientation: Int): Robot
     *
     * If this function is absent, or there is a syntax error in the script, or the script could not
     * be loaded for any other reason, the program crashes.
     */
    fun loadAlgorithm(
        scriptFile: File? = null,
        scriptString: String? = null,
    ) {
        val script = scriptFile?.readText()?.trim() ?: scriptString
        if (script == null) {
            logger.error { "No arguments provided for loadAlgorithm()" }
            return
        }

        // Make sure the scheduler is not running while the robots are replaced
        Scheduler.stop()

        // Compile the script
        val engine = ScriptEngineManager().getEngineByExtension("kts") as Compilable
        engine
            .compile(
                "import com.github.jfriemel.hybridsim.entities.*; " +
                    "import com.github.jfriemel.hybridsim.system.Commons; " +
                    "import com.badlogic.gdx.graphics.Color",
            ).eval()
        engine.compile(script).eval()

        // Replace robots in the configuration with robots from the loaded script
        invocator = engine as Invocable
        Configuration.robots.keys.forEach(::replaceRobot)
        Configuration.clearUndoQueues()
    }

    /**
     * Replaces the [Robot] at the given [node] with the robot from the most recently loaded kts
     * script. Does nothing if there is no robot at the [node] or if no kts script was loaded before
     * the call.
     */
    fun replaceRobot(node: Node) {
        val robot = Configuration.robots[node] ?: return
        Configuration.robots[node] = getAlgorithmRobot(robot)
    }

    /**
     * Returns a [Robot] with the most recently loaded algorithm and the same properties as the
     * given [robot].
     */
    fun getAlgorithmRobot(robot: Robot): Robot =
        invocator?.let { inv ->
            inv.invokeFunction("getRobot", robot.node, robot.orientation) as Robot
        } ?: robot

    /**
     * Resets to the default algorithm and replaces all [Robot]s in the [Configuration] with default
     * robots.
     */
    fun reset() {
        // Reset script function invocator so all new robots are default robots
        invocator = null

        // Replace all robots in the configuration with default robots
        Configuration.robots.keys.forEach { node ->
            val robot = Configuration.robots[node] ?: return@forEach
            // Only keep node and orientation, all other robot values are algorithm-specific
            Configuration.robots[node] = Robot(robot.node, orientation = robot.orientation)
        }
        Configuration.clearUndoQueues()
    }
}
