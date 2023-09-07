package com.github.jfriemel.hybridsim

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.github.jfriemel.hybridsim.system.Commons
import kotlin.random.Random

fun main(args: Array<String>) = Main().main(args)

/** Launches either the desktop (LWJGL3) or the CLI application. */
class Main : CliktCommand() {

    /* Valid command line arguments */
    private val noGui: Boolean by option(
        "--nogui", "-N",
        help = "Using this flag suppresses the GUI. Required for experimental simulations.",
    ).flag(default = false)

    private val algFile by option(
        "--algorithm", "-a",
        help = "A .kts script implementing a hybrid model algorithm. Examples are in the exampleAlgorithms directory.",
    ).file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val configFile by option(
        "--configuration", "-c",
        help = "A .json file containing an input configuration. Examples are in the exampleConfigurations directory.",
    ).file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val genFile by option(
        "--generator", "-g",
        help = "A .kts script implementing a configuration generator. Examples are in the exampleGenerators directory.",
    ).file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val configDir by option(
        "--configuration_dir", "-C",
        help = "Only valid when used together with --no_gui and not used with --configuration_file or --generator. " +
            "Executes the input algorithm (--algorithm) on all configuration files found in the specified directory."
    ).file(mustExist = true, canBeFile = false)

    private val numTiles by option(
        "--num_tiles", "-n",
        help = "The number of tiles to be generated by the generator given with --generator when using the GUI. For " +
            "non-gui usage (--nogui), multiple values can be given, separated by commas. Then, the input algorithm " +
            "(--algorithm) will be executed on configurations with all given values of num_tiles.",
    ).int().restrictTo(min = 1).split(",").default(listOf(50))

    private val numRobots by option(
        "--num_robots", "-k",
        help = "The number of robots to be generated by the generator given with --generator when using the GUI. For " +
            "non-gui usage (--nogui), multiple values can be given, separated by commas. Then, the input algorithm " +
            "(--algorithm) will be executed on configurations with all given values of num_robots.",
    ).int().restrictTo(min = 1).split(",").default(listOf(1))

    private val numOverhangs by option(
        "--num_overhangs", "-m",
        help = "The number of overhang tiles to be generated by the generator given with --generator when using the " +
            "GUI. No target/overhang nodes are generated when --num_overhang is not used. For non-gui usage " +
            "(--nogui), multiple values can be given, separated by commas. Then, the input algorithm " +
            "will be executed on configurations with all given values of num_overhang.",
    ).int().split(",").default(listOf(-1))

    private val numRuns by option(
        "--num_runs", "-r",
        help = "Only valid when used together with --no_gui. The number of times the input algorithm (--algorithm) " +
            "shall be executed for all given values of num_tiles, num_robots, and num_overhang.",
    ).int().restrictTo(min = 1).default(1)

    private val limit by option(
        "--limit", "-l",
        help = "Only valid when used together with --no_gui. Restricts the number of rounds in a single run. If a " +
            "run exceeds this number, it is repeated. Useful for algorithms that can get stuck in some configurations."
    ).int().restrictTo(min = 1).default(Int.MAX_VALUE)

    private val startID by option(
        "--startID", "-id",
        help = "Only valid when used together with --no_gui. ID of the first simulation run. Subsequent runs get " +
            "increasing IDs."
    ).int().restrictTo(min = 0).default(0)

    private val outputFile by option(
        "--output", "-o",
        help = "Only valid when used together with --no_gui. The path to a .csv file where a report of the algorithm " +
            "simulations shall be stored.",
    ).file(canBeDir = false)

    private val seed by option(
        "--seed", "-s",
        help = "Set a seed for randomness. Note: This may not affect randomness in loaded algorithms or " +
            "configuration generators."
    ).long()

    override fun run() {
        if (seed != null) {
            Commons.random = Random(seed!!)
        }

        if (noGui) {
            val cliArgs = CLIArguments(
                algFile,
                configFile,
                genFile,
                configDir,
                numTiles,
                numRobots,
                numOverhangs,
                numRuns,
                limit,
                startID,
                outputFile,
            )
            MainCLI(cliArgs).main()
        } else {
            val guiArgs = GUIArguments(
                algFile, configFile, genFile, numTiles[0], numRobots[0], numOverhangs[0]
            )
            Lwjgl3Application(
                MainGUI(guiArgs),
                Lwjgl3ApplicationConfiguration().apply {
                    setTitle("HybridSim")
                    useVsync(true)
                    val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
                    setForegroundFPS(displayMode.refreshRate)
                    setWindowedMode(displayMode.width * 8 / 10, displayMode.height * 8 / 10)
                    setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png")
                },
            )
        }
    }

}
