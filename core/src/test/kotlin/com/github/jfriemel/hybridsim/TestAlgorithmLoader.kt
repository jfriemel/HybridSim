package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.ui.AlgorithmLoader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_SCRIPT = """
    // Every robot has orientation 5
    fun getRobot(node: Node, orientation: Int) = Robot(node, orientation = 5)
"""

/** Test [AlgorithmLoader] functionality. */
class TestAlgorithmLoader {

    @BeforeEach
    fun init() {
        Configuration.clear()
        AlgorithmLoader.reset()
    }

    @Test
    fun `load basic algorithm script`() {
        val robot = Robot(Node(-465, -316), orientation = 0)
        Configuration.addRobot(robot)
        AlgorithmLoader.loadAlgorithm(scriptString = TEST_SCRIPT)
        Assertions.assertEquals(5, Configuration.robots[robot.node]?.orientation)
    }

    @Test
    fun `get robot with algorithm`() {
        AlgorithmLoader.loadAlgorithm(scriptString = TEST_SCRIPT)
        val robot = AlgorithmLoader.getAlgorithmRobot(Robot(Node(389, -107), orientation = 1))
        Assertions.assertEquals(5, robot.orientation)
    }

    @Test
    fun `replace robot`() {
        AlgorithmLoader.loadAlgorithm(scriptString = TEST_SCRIPT)
        val robot = Robot(Node(-773, 552), orientation = 2)
        Configuration.addRobot(robot)
        AlgorithmLoader.replaceRobot(robot.node)
        Assertions.assertEquals(5, Configuration.robots[robot.node]?.orientation)
    }

    @Test
    fun `reset algorithm`() {
        AlgorithmLoader.loadAlgorithm(scriptString = TEST_SCRIPT)
        val robot = Robot(Node(-211, 869), orientation = 3)
        AlgorithmLoader.reset()
        val algorithmRobot = AlgorithmLoader.getAlgorithmRobot(robot)
        Assertions.assertEquals(3, algorithmRobot.orientation)
    }

}
