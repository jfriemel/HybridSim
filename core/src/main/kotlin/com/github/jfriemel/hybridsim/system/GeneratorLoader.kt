package com.github.jfriemel.hybridsim.system

import java.io.File
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

private val logger = ktx.log.logger<GeneratorLoader>()

object GeneratorLoader {

    /**
     * Loads a new [Generator] from either the [scriptFile] or the [scriptString] (kts script, see
     * examples). A [Generator] is used to randomly generate a new [Configuration].
     *
     * Important: The script needs to have a getGenerator() function of the following form: fun
     * getGenerator(): Generator
     *
     * The [Generator], in turn, needs to implement the following function: override fun
     * generate(numTiles: Int, numRobots: Int, numOverhang: Int): ConfigurationDescriptor
     *
     * If either function is absent, or there is a syntax error in the script, or the script could
     * not be loaded for any other reason, the program crashes.
     */
    fun loadGenerator(scriptFile: File? = null, scriptString: String? = null) {
        val script = scriptFile?.readText()?.trim() ?: scriptString
        if (script == null) {
            logger.error { "No arguments provided for loadGenerator()" }
            return
        }

        // Compile the script
        val engine = ScriptEngineManager().getEngineByExtension("kts") as Compilable
        engine
            .compile(
                "import com.github.jfriemel.hybridsim.entities.Node; " +
                    "import com.github.jfriemel.hybridsim.system.Commons; " +
                    "import com.github.jfriemel.hybridsim.system.ConfigurationDescriptor; " +
                    "import com.github.jfriemel.hybridsim.system.Generator"
            )
            .eval()
        engine.compile(script).eval()
        Configuration.generator = (engine as Invocable).invokeFunction("getGenerator") as Generator
    }

    /** Replaces the [Configuration]'s [Generator] with the default implementation. */
    fun reset() {
        Configuration.generator = Generator()
    }
}
