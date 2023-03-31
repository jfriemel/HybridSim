package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.Configuration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

private val tileNode = Node(-5, 4)
private val robotNode = Node(13, 27)
private val labels = intArrayOf(0, 1, 2, 3, 4, 5)

class TestRobot {

    @BeforeEach
    fun initializeTilesAndRobots() {
        // Place a tile at (-5, 4)
        Configuration.tiles = mutableMapOf(tileNode to Tile(tileNode))
        // Place a robot at (13, 27)
        Configuration.robots = mutableMapOf(robotNode to Robot(robotNode, 5))
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
        assert(!robot.hasRobotAtLabel(label))
        assert(robot.moveToLabel(label))
        assert(robot.node == Node(endNodeX, endNodeY))
    }

    @ParameterizedTest(name = "neighbor label {2}")
    @CsvSource(
        "13, 28, 0",
        "12, 27, 1",
        "12, 26, 2",
        "13, 26, 3",
        "14, 26, 4",
        "14, 27, 5",
    )
    fun `can interact with robot neighbor`(startNodeX: Int, startNodeY: Int, label: Int) {
        val startNode = Node(startNodeX, startNodeY)
        val robot = Robot(Node(startNodeX, startNodeY), 0)
        assert(robot.hasRobotAtLabel(label))
        // nodeInDir(label) works because orientation = 0, so label matches global direction
        assert(robot.robotAtLabel(label) == Configuration.robots[startNode.nodeInDir(label)])
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
        assert(!robot.moveToLabel(label))
        assert(robot.node == startNode)
    }

    @Test
    fun `can interact with tile below`() {
        val robot = Robot(tileNode)
        assert(robot.isOnTile())
        assert(robot.tileBelow() == Configuration.tiles[tileNode])
    }

    @Test
    fun `cannot interact with non-existent neighbor`() {
        val robot = Robot(Node(-30, -60))
        assert(labels.all { robot.robotAtLabel(it) == null })
        assert(labels.all { robot.tileAtLabel(it) == null })
        assert(robot.tileBelow() == null)
    }

    @Test
    fun `can lift tile when on tile`() {
        val robot = Robot(tileNode, carriesTile = false)
        assert(robot.liftTile())
        assert(robot.carriesTile)
        assert(!robot.isOnTile())
    }

    @Test
    fun `can place tile when not on tile`() {
        val robot = Robot(Node(100, 200), carriesTile = true)
        assert(!robot.isOnTile())
        assert(robot.placeTile())
        assert(!robot.carriesTile)
        assert(robot.isOnTile())
    }

    @Test
    fun `can place pebble when on tile`() {
        val robot = Robot(tileNode, numPebbles = 1, maxPebbles = 1)
        assert(!robot.tileHasPebble())
        assert(robot.putPebble())
        assert(robot.tileHasPebble())
        assert(robot.numPebbles == 0)
    }

    @Test
    fun `cannot place multiple pebbles on single tile`() {
        val robot = Robot(tileNode, numPebbles = 2, maxPebbles = 2)
        assert(robot.putPebble())
        assert(robot.tileHasPebble())
        assert(!robot.putPebble())
        assert(robot.numPebbles == 1)
    }

    @Test
    fun `cannot place pebble when not on tile`() {
        val robot = Robot(Node(0, 0), numPebbles = 1, maxPebbles = 1)
        assert(!robot.putPebble())
        assert(robot.numPebbles == 1)
    }

    @Test
    fun `can take pebble from tile`() {
        Configuration.tiles[tileNode]!!.addPebble()
        val robot = Robot(tileNode, numPebbles = 0, maxPebbles = 1)
        assert(robot.takePebble())
        assert(robot.numPebbles == 1)
    }

    @Test
    fun `cannot take more pebbles than maxPebbles`() {
        Configuration.tiles[tileNode]!!.addPebble()
        val robot = Robot(tileNode, numPebbles = 0, maxPebbles = 0)
        assert(!robot.takePebble())
        assert(robot.numPebbles == 0)
    }

}
