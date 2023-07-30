package com.github.jfriemel.hybridsim

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.jfriemel.hybridsim.cli.FullSequentialScheduler
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.GeneratorLoader
import com.github.jfriemel.hybridsim.ui.AlgorithmLoader
import java.io.File

class MainCLI(
    private val algFile: File? = null,
    private val configFile: File? = null,
    private val genFile: File? = null,
    private val numTiles: List<Int>,
    private val numRobots: List<Int>,
    private val numOverhang: List<Int>,
    private val numRuns: Int,
    private val threshold: Int,
    private val outputFile: File?,
) {

    fun main() {
        if (algFile == null || (genFile == null && configFile == null)) {
            System.err.println("Cannot run without algorithm script and either generator script or configuration file!")
            return
        }

        // Load algorithm
        algFile.let { file -> AlgorithmLoader.loadAlgorithm(file) }

        // Load generator
        genFile?.let { file -> GeneratorLoader.loadGenerator(file) }

        // Write CSV header
        outputFile?.let { file ->
            csvWriter().writeAll(listOf(listOf("id", "numTiles", "numRobots", "numOverhang", "rounds")), file)
        }

        // Run algorithm on given configuration
        if (configFile != null) {
            val configJson = configFile.readText()
            Configuration.loadConfiguration(configJson)
            val n = Configuration.tiles.keys.size
            val k = Configuration.robots.keys.size
            val m = Configuration.targetNodes.size
            for (id in 0..<numRuns) {
                singleRun(configJson, id, n, k, m, threshold, outputFile)
            }
            return
        }

        // Run algorithm on generated configurations
        var id = 0
        for (n in numTiles) {
            for (k in numRobots) {
                for (m in numOverhang) {
                    repeat(numRuns) {
                        singleRun(null, id, n, k, m, threshold, outputFile)
                        id++
                    }
                }
            }
        }
    }

}

/**
 * Loads the [Configuration] from [configJson] or generates a configuration if [configJson] is null. Then performs a
 * full sequential scheduler run on the current [Configuration] until termination. Repeats the run if the number of
 * rounds reaches [threshold] before termination. Then writes the number of rounds for the current configuration to the
 * [outputFile] csv file.
 */
private fun singleRun(configJson: String?, id: Int, n: Int, k: Int, m: Int, threshold: Int, outputFile: File?) {
    var rounds: Int?
    do {
        if (configJson != null) {
            Configuration.loadConfiguration(configJson)
        } else {
            Configuration.generate(n, k, m)
        }
        rounds = FullSequentialScheduler.run(threshold)
        if (rounds == null) {
            println("id=$id, n=$n, k=$k, m=$m, threshold $threshold reached, run aborted!")
        }
    } while (rounds == null)
    println("id=$id, n=$n, k=$k, m=$m, rounds=$rounds")
    outputFile?.let { file ->
        csvWriter().writeAll(listOf(listOf(id, n, k, m, rounds)), file, append = true)
    }
}
