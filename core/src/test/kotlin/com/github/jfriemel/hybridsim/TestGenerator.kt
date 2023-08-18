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
        "100, 101",
        "676, 917",
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
        "-138, 25",
    )
    fun `generate fewer than one tile`(numTiles: Int, numRobots: Int) {
        Configuration.generate(numTiles, numRobots)
        Assertions.assertEquals(0, Configuration.tiles.size)
        Assertions.assertEquals(0, Configuration.robots.size)
    }

    @ParameterizedTest(name = "numTiles = {0}, numRobots = {1}, numOverhang = {3}")
    @CsvSource(
        "255, 1, 254",
        "397, 97, 0",
        "467, 427, 80",
        "934, 209, 610",
    )
    fun `generate valid shape reconfiguration instance`(numTiles: Int, numRobots: Int, numOverhang: Int) {
        Configuration.generate(numTiles, numRobots, numOverhang)
        Assertions.assertEquals(numTiles, Configuration.tiles.size)
        Assertions.assertEquals(numRobots, Configuration.robots.size)
        Assertions.assertEquals(numOverhang, Configuration.targetNodes.minus(Configuration.tiles.keys).size)
    }

    @ParameterizedTest(name = "numTiles = {0}, numRobots = {1}, numOverhang = {3}")
    @CsvSource(
        "10, 4, 10",
        "913, 273, 1045",
    )
    fun `generate too many overhang tiles`(numTiles: Int, numRobots: Int, numOverhang: Int) {
        Configuration.generate(numTiles, numRobots, numOverhang)
        Assertions.assertEquals(numTiles, Configuration.tiles.size)
        Assertions.assertEquals(numRobots, Configuration.robots.size)
        Assertions.assertEquals(numTiles - 1, Configuration.targetNodes.minus(Configuration.tiles.keys).size)
    }

}
