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
private val tileRobotNode = Node(69, 46)

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

        // Place a robot with a tile at (13, 27)
        Configuration.addRobot(Robot(robotNode).apply { carriesTile = true })

        // Place a robot on a tile at (69, 46)
        Configuration.addTile(Tile(tileRobotNode))
        Configuration.addRobot(Robot(tileRobotNode))

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

        // Enclose (364, 892) by empty target nodes
        Configuration.addTarget(Node(364, 891))
        Configuration.addTarget(Node(365, 892))
        Configuration.addTarget(Node(365, 893))
        Configuration.addTarget(Node(364, 893))
        Configuration.addTarget(Node(363, 893))
        Configuration.addTarget(Node(363, 892))
    }

    @ParameterizedTest(name = "movement label {0}")
    @CsvSource(
        "0, -5, 3",
        "1, -4, 3",
        "2, -4, 4",
        "3, -5, 5",
        "4, -6, 4",
        "5, -6, 3",
    )
    fun `can move to free node from tile`(
        label: Int,
        endNodeX: Int,
        endNodeY: Int,
    ) {
        val robot = Robot(tileNode, orientation = 0)
        Assertions.assertFalse(robot.hasRobotAtLabel(label))
        Assertions.assertTrue(robot.canMoveToLabel(label))
        Assertions.assertTrue(robot.moveToLabel(label))
        Assertions.assertEquals(Node(endNodeX, endNodeY), robot.node)
    }

    @ParameterizedTest(name = "movement label {2}")
    @CsvSource(
        "-5, 5, 0",
        "-6, 4, 1",
        "-6, 3, 2",
        "-5, 3, 3",
        "-4, 3, 4",
        "-4, 4, 5",
    )
    fun `can move to tile`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.hasTileAtLabel(label))
        Assertions.assertTrue(robot.canMoveToLabel(label))
        Assertions.assertTrue(robot.moveToLabel(label))
        Assertions.assertEquals(tileNode, robot.node)
    }

    @ParameterizedTest(name = "movement label {2}")
    @CsvSource(
        "-6, 4, 0, -6, 3",
        "-5, 5, 1, -4, 4",
        "-5, 3, 2, -4, 3",
        "-4, 3, 3, -4, 4",
        "-5, 3, 4, -6, 3",
        "-5, 5, 5, -6, 4",
    )
    fun `can move to empty node next to tile`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
        endNodeX: Int,
        endNodeY: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.canMoveToLabel(label))
        Assertions.assertTrue(robot.moveToLabel(label))
        Assertions.assertEquals(Node(endNodeX, endNodeY), robot.node)
    }

    @ParameterizedTest(name = "movement label {2}")
    @CsvSource(
        "14, 27, 0, 14, 26",
        "12, 26, 1, 13, 26",
        "12, 27, 2, 13, 28",
        "12, 26, 3, 12, 27",
        "13, 26, 4, 12, 26",
        "13, 28, 5, 12, 27",
    )
    fun `can move to empty node next to robot carrying tile`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
        endNodeX: Int,
        endNodeY: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.canMoveToLabel(label))
        Assertions.assertTrue(robot.moveToLabel(label))
        Assertions.assertEquals(Node(endNodeX, endNodeY), robot.node)
    }

    @ParameterizedTest(name = "movement label {0}")
    @ValueSource(ints = [0, 1, 2, 3, 4, 5])
    fun `cannot move to disconnected free node`(label: Int) {
        val startNode = Node(93, -668)
        val robot = Robot(startNode)
        Assertions.assertFalse(robot.hasRobotAtLabel(label))
        Assertions.assertFalse(robot.canMoveToLabel(label))
        Assertions.assertFalse(robot.moveToLabel(label))
        Assertions.assertEquals(startNode, robot.node)
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
    fun `can switch with robot neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        // Initialize
        val startNode = Node(startNodeX, startNodeY)
        val robot = Robot(startNode, 0)
        Configuration.addRobot(robot)
        val robotNbr = Configuration.robots[robotNode]

        // Switch robots
        Assertions.assertTrue(robot.switchWithRobotNbr(label))

        // Robots have been switched correctly
        Assertions.assertEquals(robotNode, robot.node)
        Assertions.assertEquals(startNode, robotNbr!!.node)
        Assertions.assertFalse(robot.carriesTile)
        Assertions.assertTrue(robotNbr.carriesTile)
        Assertions.assertEquals(robot, Configuration.robots[robotNode])
        Assertions.assertEquals(robotNbr, Configuration.robots[startNode])
    }

    @Test
    fun `cannot switch with non-existent robot neighbor`() {
        val robotNode = Node(744, -380)
        val robot = Robot(robotNode)
        robot.labels.firstOrNull { label -> robot.hasTileAtLabel(label) }?.let {  } ?: println("no")
        robot.labels.forEach { label ->
            Assertions.assertFalse(robot.switchWithRobotNbr(label))
            Assertions.assertEquals(robotNode, robot.node)
        }
    }

    @Test
    fun `is at a boundary`() {
        val robot = Robot(Node(617, -966))
        Assertions.assertTrue(robot.isAtBoundary())
    }

    @Test
    fun `is not at a boundary`() {
        val robot = Robot(Node(258, 257))
        Assertions.assertFalse(robot.isAtBoundary())
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
    fun `can interact with robot neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val startNode = Node(startNodeX, startNodeY)
        val robot = Robot(startNode, 0)
        Assertions.assertTrue(robot.hasRobotAtLabel(label))
        Assertions.assertEquals(Configuration.robots[robotNode], robot.robotAtLabel(label))
        Assertions.assertTrue(robot.hasRobotNbr())
        Assertions.assertEquals(label, robot.robotNbrLabel())
        Assertions.assertEquals(Configuration.robots[robotNode], robot.robotNbr())
        Assertions.assertEquals(listOf(label), robot.allRobotNbrLabels())
        Assertions.assertEquals(listOf(Configuration.robots[robotNode]), robot.allRobotNbrs())
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
    fun `can interact with hanging robot neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), 0)
        Assertions.assertTrue(robot.hasHangingRobotNbr())
        Assertions.assertEquals(label, robot.hangingRobotNbrLabel())
        Assertions.assertEquals(Configuration.robots[robotNode], robot.hangingRobotNbr())
        Assertions.assertEquals(listOf(label), robot.allHangingRobotNbrLabels())
        Assertions.assertEquals(
            listOf(Configuration.robots[robotNode]),
            robot.allHangingRobotNbrs(),
        )
    }

    @ParameterizedTest(name = "neighbor label {2}")
    @CsvSource(
        "69, 47, 0",
        "68, 46, 1",
        "68, 45, 2",
        "69, 45, 3",
        "70, 45, 4",
        "70, 46, 5",
    )
    fun `can distinguish non-hanging robot neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), 0)

        // Checks for robot neighbor
        Assertions.assertTrue(robot.hasRobotAtLabel(label))
        Assertions.assertEquals(Configuration.robots[tileRobotNode], robot.robotAtLabel(label))
        Assertions.assertTrue(robot.hasRobotNbr())
        Assertions.assertEquals(label, robot.robotNbrLabel())
        Assertions.assertEquals(Configuration.robots[tileRobotNode], robot.robotNbr())

        // Checks for hanging robot neighbor
        Assertions.assertFalse(robot.hasHangingRobotNbr())
        Assertions.assertNull(robot.hangingRobotNbrLabel())
        Assertions.assertNull(robot.hangingRobotNbr())
        Assertions.assertTrue(robot.allHangingRobotNbrLabels().isEmpty())
        Assertions.assertTrue(robot.allHangingRobotNbrs().isEmpty())
    }

    @Test
    fun `cannot see non-existent neighbors`() {
        val robot = Robot(Node(-536, 370))
        robot.labels.forEach { label ->
            Assertions.assertFalse(robot.hasTileAtLabel(label))
            Assertions.assertFalse(robot.hasRobotAtLabel(label))
        }
        Assertions.assertFalse(robot.hasTileNbr())
        Assertions.assertNull(robot.tileNbrLabel())
        Assertions.assertNull(robot.tileNbr())
        Assertions.assertFalse(robot.hasRobotNbr())
        Assertions.assertNull(robot.robotNbrLabel())
        Assertions.assertNull(robot.robotNbr())
        Assertions.assertTrue(robot.allRobotNbrLabels().isEmpty())
        Assertions.assertTrue(robot.allRobotNbrs().isEmpty())
        Assertions.assertTrue(robot.allHangingRobotNbrLabels().isEmpty())
        Assertions.assertTrue(robot.allHangingRobotNbrs().isEmpty())
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
    fun `cannot move to occupied node`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
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
    fun `can see tile neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), 0)
        Assertions.assertTrue(robot.hasTileAtLabel(label))
        Assertions.assertEquals(Configuration.tiles[tileNode], robot.tileAtLabel(label))
        Assertions.assertTrue(robot.hasTileNbr())
        Assertions.assertEquals(label, robot.tileNbrLabel())
        Assertions.assertEquals(Configuration.tiles[tileNode], robot.tileNbr())
    }

    @Test
    fun `can interact with tile below`() {
        val robot = Robot(tileNode)
        Assertions.assertTrue(robot.isOnTile())
        Assertions.assertEquals(Configuration.tiles[tileNode], robot.tileBelow())
    }

    @Test
    fun `cannot interact with non-existent neighbor`() {
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
    fun `label is target`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
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
        "254, 257, 1", // one tile neighbor
        "258, 254, 1", // no tile neighbors
        "255, 256, 2",
        "256, 256, 3",
    )
    fun `correct number of empty boundaries`(
        startNodeX: Int,
        startNodeY: Int,
        numBoundaries: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY))
        Assertions.assertEquals(numBoundaries, robot.numBoundaries(tileBoundaries = false))
    }

    @ParameterizedTest(name = "number of boundaries = {2}")
    @CsvSource(
        "258, 254, 0",
        "254, 257, 1", // one tile neighbor
        "258, 257, 1", // surrounded by tiles
        "255, 256, 2",
        "256, 256, 3",
    )
    fun `correct number of tile boundaries`(
        startNodeX: Int,
        startNodeY: Int,
        numBoundaries: Int,
    ) {
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
    fun `has target tile neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.hasTargetTileNbr())
        Assertions.assertFalse(robot.hasOverhangNbr())
        Assertions.assertEquals(label, robot.targetTileNbrLabel())
        Assertions.assertEquals(robot.tileAtLabel(label), robot.targetTileNbr())
    }

    @Test
    fun `has no target tile neighbor`() {
        val robot = Robot(Node(-135, -808))
        Assertions.assertFalse(robot.hasTargetTileNbr())
        Assertions.assertNull(robot.targetTileNbrLabel())
        Assertions.assertNull(robot.targetTileNbr())
    }

    @ParameterizedTest(name = "empty target node at label {2}")
    @CsvSource(
        "-22, -5, 0",
        "-23, -5, 1",
        "-23, -6, 2",
        "-22, -7, 3",
        "-21, -6, 4",
        "-21, -5, 5",
    )
    fun `has empty target node neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.hasEmptyTargetNbr())
        Assertions.assertEquals(label, robot.emptyTargetNbrLabel())
    }

    @Test
    fun `has no empty target node neighbor`() {
        val robot = Robot(Node(-236, 576))
        Assertions.assertFalse(robot.hasEmptyTargetNbr())
        Assertions.assertNull(robot.emptyTargetNbrLabel())
    }

    @Test
    fun `has empty non-target node neighbor`() {
        val robot = Robot(Node(-121, -53))
        Assertions.assertTrue(robot.hasEmptyNonTargetNbr())
        Assertions.assertNotNull(robot.emptyNonTargetNbrLabel())
    }

    @Test
    fun `has no empty non-target node neighbor`() {
        val robot = Robot(Node(364, 892))
        Assertions.assertFalse(robot.hasEmptyNonTargetNbr())
        Assertions.assertNull(robot.emptyNonTargetNbrLabel())
    }

    @ParameterizedTest(name = "overhang at label {2}")
    @CsvSource(
        "-5, 5, 0",
        "-6, 4, 1",
        "-6, 3, 2",
        "-5, 3, 3",
        "-4, 3, 4",
        "-4, 4, 5",
    )
    fun `has overhang neighbor`(
        startNodeX: Int,
        startNodeY: Int,
        label: Int,
    ) {
        val robot = Robot(Node(startNodeX, startNodeY), orientation = 0)
        Assertions.assertTrue(robot.hasOverhangNbr())
        Assertions.assertFalse(robot.hasTargetTileNbr())
        Assertions.assertEquals(label, robot.overhangNbrLabel())
        Assertions.assertEquals(robot.tileAtLabel(label), robot.overhangNbr())
    }

    @Test
    fun `has no overhang neighbor`() {
        val robot = Robot(Node(520, 878))
        Assertions.assertFalse(robot.hasOverhangNbr())
        Assertions.assertNull(robot.overhangNbrLabel())
        Assertions.assertNull(robot.overhangNbr())
    }

    @Test
    fun `triggerActivate() runs activate() and adds undo step`() {
        val robot = RobotTestImpl(Node(tileNode.x, tileNode.y + 1), 3)
        robot.triggerActivate()
        Assertions.assertEquals(tileNode, robot.node)
        Assertions.assertEquals(1, Configuration.undoSteps())
    }
}
