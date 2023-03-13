package com.github.jfriemel.hybridsim.ui

import com.github.jfriemel.hybridsim.Configuration
import com.github.jfriemel.hybridsim.Scheduler
import com.github.jfriemel.hybridsim.entities.Robot
import java.io.File
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

object AlgorithmLoader {

    /**
     * Loads a new algorithm from the [scriptFile] (kts script, see examples). The script implements Robot and overrides
     * the existing activate() function.
     *
     * Important: The script needs to have a getRobot() function of the following form:
     *     fun getRobot(orientation: Int, node: Node, carriesTile: Boolean, numPebbles: Int, maxPebbles: Int): Robot
     *
     * If this function is absent, or there is a syntax error in the script or the script could not be loaded for any
     * other reason, the program crashes.
     */
    fun loadAlgorithm(scriptFile: File) {
        Scheduler.stop()  // Make sure the scheduler is not running while the robots are replaced
        val engine = ScriptEngineManager().getEngineByExtension("kts") as Compilable
        engine.compile("import com.github.jfriemel.hybridsim.entities.*").eval()  // Default import
        engine.compile(scriptFile.readText().trim()).eval()
        val invocator = engine as Invocable
        for (key in Configuration.robots.keys) {
            val robot = Configuration.robots[key] ?: continue
            Configuration.robots[key] = invocator.invokeFunction(
                "getRobot", robot.orientation, robot.node, robot.carriesTile, robot.numPebbles, robot.maxPebbles
            ) as Robot
        }
    }

}
