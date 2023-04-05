package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

private val tileNode = Node(-5, 4)
private val robotNode = Node(13, 27)

class TestRobot {

    @BeforeEach
    fun init() {
        Configuration.clear()
        // Place a tile at (-5, 4)
        Configuration.addTile(Tile(tileNode))
        // Place a robot at (13, 27)
        Configuration.addRobot(Robot(robotNode))
    }

    @ParameterizedTest(name = "movement label {0}")
    @CsvSource(
        "0, 3, 5",
        "1, 2, 4",
        "2, 2, 3",
        "3, 3, 3",
        "4, 4, 3",
        "5, 4, 4",
    )
    fun `can move to free node`(label: Int, endNodeX: Int, endNodeY: Int) {
        val robot = Robot(Node(3, 4), 3)
        Assertions.assertFalse(robot.hasRobotAtLabel(label))
        Assertions.assertTrue(robot.moveToLabel(label))
        Assertions.assertEquals(Node(endNodeX, endNodeY), robot.node)
    }

    @ParameterizedTest(name = "neighbour label {2}")
    @CsvSource(
        "13, 28, 0",
        "12, 27, 1",
        "12, 26, 2",
        "13, 26, 3",
        "14, 26, 4",
        "14, 27, 5",
    )
    fun `can interact with robot neighbour`(startNodeX: Int, startNodeY: Int, label: Int) {
        val startNode = Node(startNodeX, startNodeY)
        val robot = Robot(Node(startNodeX, startNodeY), 0)
        Assertions.assertTrue(robot.hasRobotAtLabel(label))
        // nodeInDir(label) works because orientation = 0, so label matches global direction
        Assertions.assertEquals(Configuration.robots[startNode.nodeInDir(label)], robot.robotAtLabel(label))
    }

    @Test
    fun `cannot see non-existent neighbours`() {
        val robot = Robot(Node(-536, 370))
        robot.labels.forEach {
            Assertions.assertFalse(robot.hasTileAtLabel(it))
            Assertions.assertFalse(robot.hasRobotAtLabel(it))
        }
    }

    @ParameterizedTest(name = "movement label {2}")
    @CsvSource(
        "13, 28, 0",
        "12, 27, 1",
        "12, 26, 2",
        "13, 26, 3",
        "14, 26, 4",
        "14, 27, 5",
    )
    fun `cannot move to occupied node`(startNodeX: Int, startNodeY: Int, label: Int) {
        val startNode = Node(startNodeX, startNodeY)
        val robot = Robot(Node(startNodeX, startNodeY), 0)
        Assertions.assertFalse(robot.moveToLabel(label))
        Assertions.assertEquals(startNode, robot.node)
    }

    @ParameterizedTest(name = "tile at label {2}")
    @CsvSource(
        "-5, 5, 0",
        "-6, 4, 1",
        "-6, 3, 2",
        "-5, 3, 3",
        "-4, 3, 4",
        "-4, 4, 5",
    )
    fun `can see tile neighbour`(startNodeX: Int, startNodeY: Int, label: Int) {
        val robot = Robot(Node(startNodeX, startNodeY), 0)
        Assertions.assertTrue(robot.hasTileAtLabel(label))
    }

    @Test
    fun `can interact with tile below`() {
        val robot = Robot(tileNode)
        Assertions.assertTrue(robot.isOnTile())
        Assertions.assertEquals(Configuration.tiles[tileNode], robot.tileBelow())
    }

    @Test
    fun `cannot interact with non-existent neighbour`() {
        val robot = Robot(Node(-30, -60))
        robot.labels.forEach { label -> Assertions.assertNull(robot.robotAtLabel(label)) }
        robot.labels.forEach { label -> Assertions.assertNull(robot.tileAtLabel(label)) }
        Assertions.assertNull(robot.tileBelow())
    }

    @Test
    fun `can lift tile when on tile`() {
        val robot = Robot(tileNode, carriesTile = false)
        Assertions.assertTrue(robot.liftTile())
        Assertions.assertTrue(robot.carriesTile)
        Assertions.assertFalse(robot.isOnTile())
    }

    @Test
    fun `can place tile when not on tile`() {
        val robot = Robot(Node(100, 200), carriesTile = true)
        Assertions.assertFalse(robot.isOnTile())
        Assertions.assertTrue(robot.placeTile())
        Assertions.assertFalse(robot.carriesTile)
        Assertions.assertTrue(robot.isOnTile())
    }

    @Test
    fun `can place pebble when on tile`() {
        val robot = Robot(tileNode, numPebbles = 1, maxPebbles = 1)
        Assertions.assertFalse(robot.tileHasPebble())
        Assertions.assertTrue(robot.putPebble())
        Assertions.assertTrue(robot.tileHasPebble())
        Assertions.assertEquals(0, robot.numPebbles)
    }

    @Test
    fun `cannot place multiple pebbles on single tile`() {
        val robot = Robot(tileNode, numPebbles = 2, maxPebbles = 2)
        Assertions.assertTrue(robot.putPebble())
        Assertions.assertTrue(robot.tileHasPebble())
        Assertions.assertFalse(robot.putPebble())
        Assertions.assertEquals(1, robot.numPebbles)
    }

    @Test
    fun `cannot place pebble when not on tile`() {
        val robot = Robot(Node(0, 0), numPebbles = 1, maxPebbles = 1)
        Assertions.assertFalse(robot.putPebble())
        Assertions.assertEquals(1, robot.numPebbles)
    }

    @Test
    fun `can take pebble from tile`() {
        Configuration.tiles[tileNode]!!.addPebble()
        val robot = Robot(tileNode, numPebbles = 0, maxPebbles = 1)
        Assertions.assertTrue(robot.takePebble())
        Assertions.assertEquals(1, robot.numPebbles)
    }

    @Test
    fun `cannot take more pebbles than maxPebbles`() {
        Configuration.tiles[tileNode]!!.addPebble()
        val robot = Robot(tileNode, numPebbles = 0, maxPebbles = 0)
        Assertions.assertFalse(robot.takePebble())
        Assertions.assertEquals(0, robot.numPebbles)
    }

}
