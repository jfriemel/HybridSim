package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.GeneratorLoader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_SCRIPT = """
fun getGenerator(): Generator = GeneratorImpl()

class GeneratorImpl(): Generator() {
    override fun generate(numTiles: Int, numRobots: Int, numOverhang: Int): ConfigurationDescriptor {
        return ConfigurationDescriptor(mutableSetOf(Node(0, 0)), mutableSetOf(Node(0, 1)), mutableSetOf(Node(2, 3)))
    }
}
"""

/** Test [GeneratorLoader] functionality. */
class TestGeneratorLoader {

    @BeforeEach
    fun init() {
        Configuration.clear()
        GeneratorLoader.reset()
    }

    @Test
    fun `load basic generator script`() {
        GeneratorLoader.loadGenerator(scriptString = TEST_SCRIPT)
        Configuration.generate(10, 5, 5)
        Assertions.assertEquals(mutableSetOf(Node(0, 0)), Configuration.tiles.keys)
        Assertions.assertEquals(mutableSetOf(Node(0, 1)), Configuration.robots.keys)
        Assertions.assertEquals(mutableSetOf(Node(2, 3)), Configuration.targetNodes)
    }

    @Test
    fun `reset generator`() {
        GeneratorLoader.loadGenerator(scriptString = TEST_SCRIPT)
        GeneratorLoader.reset()
        Configuration.generate(10, 5, 5)
        Assertions.assertEquals(10, Configuration.tiles.size)
        Assertions.assertEquals(5, Configuration.robots.size)
        Assertions.assertEquals(10, Configuration.targetNodes.size)
    }

}
