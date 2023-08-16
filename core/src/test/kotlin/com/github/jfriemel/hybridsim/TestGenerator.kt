package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.Generator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Test [Generator] functionality. */
class TestGenerator {

    @BeforeEach
    fun init() {
        Configuration.clear()
    }

    @ParameterizedTest(name = "numTiles = {0}, numRobots = {1}")
    @CsvSource(
        "100, 2",
        "1, 1",
        "1, 0",
        "255, 254",
        "805, 571",
    )
    fun `generate valid configuration`(numTiles: Int, numRobots: Int) {
        Configuration.generate(numTiles, numRobots)
        Assertions.assertEquals(numTiles, Configuration.tiles.size)
        Assertions.assertEquals(numRobots, Configuration.robots.size)
    }

    @ParameterizedTest(name = "numTiles = {0}, numRobots = {1}")
    @CsvSource(
        "676, 917",
        "400, 706",
        "109, 506",
        "43, 891",
    )
    fun `generate too many robots`(numTiles: Int, numRobots: Int) {
        Configuration.generate(numTiles, numRobots)
        Assertions.assertEquals(numTiles, Configuration.tiles.size)
        Assertions.assertEquals(numTiles, Configuration.robots.size)
    }

    @ParameterizedTest(name = "numTiles = {0}, numRobots = {1}")
    @CsvSource(
        "0, 0",
        "0, 100",
        "-138, 959",
        "-813, 455",
    )
    fun `generate fewer than one tile`(numTiles: Int, numRobots: Int) {
        Configuration.generate(numTiles, numRobots)
        Assertions.assertEquals(0, Configuration.tiles.size)
        Assertions.assertEquals(0, Configuration.robots.size)
    }

}
