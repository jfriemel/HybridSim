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
import org.junit.jupiter.params.provider.ValueSource

private val tileNode = Node(-5, 4)
private val targetTileNode = Node(8, -5)
private val targetNode = Node(-22, -6)
private val robotNode = Node(13, 27)

/** Implementation of [Robot] for testing [Robot.triggerActivate]. */
private class RobotTestImpl(node: Node, orientation: Int) : Robot(node, orientation) {
    override fun activate() {
        moveToLabel(3)
    }
}

/** Test [Robot] functionality. */
class TestRobot {

    @BeforeEach
    fun init() {
        Configuration.clear()

        // Place a non-target tile at (-5, 4)
        Configuration.addTile(Tile(tileNode))

        // Place a target tile at (8, -5)
        Configuration.addTile(Tile(targetTileNode))
        Configuration.addTarget(targetTileNode)

        // Make a target at the empty node (-22, -6)
        Configuration.addTarget(targetNode)

        // Place a robot at (13, 27)
        Configuration.addRobot(Robot(robotNode))

        // Place three boundary tiles around (256, 256)
        Configuration.addTile(Tile(Node(256, 255)))
        Configuration.addTile(Tile(Node(255, 257)))
        Configuration.addTile(Tile(Node(257, 257)))

        // Completely enclose (258, 257)
        Configuration.addTile(Tile(Node(258, 256)))
        Configuration.addTile(Tile(Node(259, 257)))
        Configuration.addTile(Tile(Node(259, 258)))
        Configuration.addTile(Tile(Node(258, 258)))
        Configuration.addTile(Tile(Node(257, 258)))
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

    @Test
    fun `is on target`() {
        val robot = Robot(targetNode)
        Assertions.assertTrue(robot.isOnTarget())
    }

    @Test
    fun `is not on target`() {
        val robot = Robot(Node(203, -970))
        Assertions.assertFalse(robot.isOnTarget())
    }

    @ParameterizedTest(name = "target at label {2}")
    @CsvSource(
        "-22, -5, 0",
        "-23, -5, 1",
        "-23, -6, 2",
        "-22, -7, 3",
        "-21, -6, 4",
        "-21, -5, 5",
    )
    fun `label is target`(startNodeX: Int, startNodeY: Int, label: Int) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.labelIsTarget(label))
    }

    @ParameterizedTest(name = "no target at label {0}")
    @ValueSource(ints = [0, 1, 2, 3, 4, 5])
    fun `label is not target`(label: Int) {
        val robot = Robot(Node(148, 819))
        Assertions.assertFalse(robot.labelIsTarget(label))
    }

    @ParameterizedTest(name = "number of boundaries = {2}")
    @CsvSource(
        "258, 257, 0",
        "254, 257, 1",  // one tile neighbour
        "258, 254, 1",  // no tile neighbours
        "255, 256, 2",
        "256, 256, 3",
    )
    fun `correct number of empty boundaries`(startNodeX: Int, startNodeY: Int, numBoundaries: Int) {
        val robot = Robot(Node(startNodeX, startNodeY))
        Assertions.assertEquals(numBoundaries, robot.numBoundaries(tileBoundaries = false))
    }

    @ParameterizedTest(name = "number of boundaries = {2}")
    @CsvSource(
        "258, 254, 0",
        "254, 257, 1",  // one tile neighbour
        "258, 257, 1",  // surrounded by tiles
        "255, 256, 2",
        "256, 256, 3",
    )
    fun `correct number of tile boundaries`(startNodeX: Int, startNodeY: Int, numBoundaries: Int) {
        val robot = Robot(Node(startNodeX, startNodeY))
        Assertions.assertEquals(numBoundaries, robot.numBoundaries(tileBoundaries = true))
    }

    @ParameterizedTest(name = "target tile at label {2}")
    @CsvSource(
        "8, -4, 0",
        "7, -4, 1",
        "7, -5, 2",
        "8, -6, 3",
        "9, -5, 4",
        "9, -4, 5",
    )
    fun `has target tile neighbour`(startNodeX: Int, startNodeY: Int, label: Int) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.hasTargetTileNbr())
        Assertions.assertFalse(robot.hasOverhangTileNbr())
        val tileLabel = robot.targetTileNbrLabel()
        Assertions.assertEquals(label, tileLabel)
        Assertions.assertEquals(robot.tileAtLabel(label), robot.targetTileNbr())
    }

    @ParameterizedTest(name = "overhang tile at label {2}")
    @CsvSource(
        "-5, 5, 0",
        "-6, 4, 1",
        "-6, 3, 2",
        "-5, 3, 3",
        "-4, 3, 4",
        "-4, 4, 5",
    )
    fun `has overhang tile neighbour`(startNodeX: Int, startNodeY: Int, label: Int) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.hasOverhangTileNbr())
        Assertions.assertFalse(robot.hasTargetTileNbr())
        val tileLabel = robot.overhangTileNbrLabel()
        Assertions.assertEquals(label, tileLabel)
        Assertions.assertEquals(robot.tileAtLabel(label),robot.overhangTileNbr())
    }

    @Test
    fun `triggerActivate() runs activate() and adds undo step`() {
        val robot = RobotTestImpl(Node(335, -302), 3)
        robot.triggerActivate()
        Assertions.assertEquals(Node(335, -303), robot.node)
        Assertions.assertEquals(1, Configuration.undoSteps())
    }

}
