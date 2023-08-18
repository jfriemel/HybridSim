package com.github.jfriemel.hybridsim

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.jfriemel.hybridsim.cli.FullSequentialScheduler
import com.github.jfriemel.hybridsim.system.AlgorithmLoader
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.GeneratorLoader
import java.io.File

data class CLIArguments(
    val algFile: File? = null,
    val configFile: File? = null,
    val genFile: File? = null,
    val configDir: File? = null,
    val numTiles: List<Int>,
    val numRobots: List<Int>,
    val numOverhang: List<Int>,
    val numRuns: Int,
    val limit: Int,
    val startID: Int,
    val outputFile: File?,
)

class MainCLI(private val args: CLIArguments) {

    fun main() {
        if (args.algFile == null || (args.genFile == null && args.configFile == null && args.configDir == null)) {
            System.err.println(
                "Cannot run without algorithm script and either generator script or configuration file or directory!"
            )
            return
        }

        // Load algorithm
        args.algFile.let { file -> AlgorithmLoader.loadAlgorithm(file) }

        // Load generator
        args.genFile?.let { file -> GeneratorLoader.loadGenerator(file) }

        // Write CSV header
        args.outputFile?.let { file ->
            if (file.exists()) {
                System.err.println("Output file $file already exists, aborting!")
                return
            }
            file.parentFile.mkdirs()
            csvWriter().writeAll(listOf(listOf("id", "numTiles", "numRobots", "numOverhang", "rounds")), file)
        }

        // Run algorithm on given configuration
        if (args.configFile != null) {
            val configJson = args.configFile.readText()

            for (id in args.startID..<(args.numRuns + args.startID)) {
                Configuration.loadConfiguration(configJson)
                val n = Configuration.tiles.keys.size
                val k = Configuration.robots.keys.size
                val m = Configuration.tiles.keys.minus(Configuration.targetNodes).size
                singleRun(id, n, k, m, args.limit, args.outputFile)
            }
            return
        }

        // Run algorithm on all configurations in specified configuration directory
        var id = args.startID
        if (args.configDir != null) {
            args.configDir.listFiles()?.filter { file ->
                file.toString().endsWith(".json")
            }?.forEach { file ->
                val configJson = file.readText()
                repeat(args.numRuns) {
                    Configuration.loadConfiguration(configJson)
                    val n = Configuration.tiles.keys.size
                    val k = Configuration.robots.keys.size
                    val m = Configuration.tiles.keys.minus(Configuration.targetNodes).size
                    singleRun(id++, n, k, m, args.limit, args.outputFile)
                }
            }
            return
        }

        // Run algorithm on generated configurations
        id = args.startID
        for (n in args.numTiles) {
            for (k in args.numRobots) {
                for (m in args.numOverhang) {
                    repeat(args.numRuns) {
                        Configuration.generate(n, k, m)
                        singleRun(id++, n, k, m, args.limit, args.outputFile)
                    }
                }
            }
        }
    }

}

/**
 * Performs a full sequential scheduler run on the current [Configuration] until termination or until the number of
 * rounds reaches the specified [limit]. Finally, writes the number of rounds for the current configuration to the
 * [outputFile] csv file.
 */
private fun singleRun(id: Int, n: Int, k: Int, m: Int, limit: Int, outputFile: File?) {
    val rounds: Int? = FullSequentialScheduler.run(limit)
    if (rounds == null) {
        println("id=$id, n=$n, k=$k, m=$m, rounds=$limit (limit!)")
    } else {
        println("id=$id, n=$n, k=$k, m=$m, rounds=$rounds")
    }
    outputFile?.let { file ->
        csvWriter().writeAll(listOf(listOf(id, n, k, m, rounds)), file, append = true)
    }
}
