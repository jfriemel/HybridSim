package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Test [Configuration] functionality. */
class TestConfiguration {

    @BeforeEach
    fun init() {
        Configuration.clear()
    }

    @Test
    fun `add tile`() {
        val tile = Tile(Node(10, -5))
        Configuration.addTile(tile)
        Assertions.assertEquals(tile, Configuration.tiles[tile.node])
    }

    @Test
    fun `remove tile`() {
        val tile = Tile(Node(-5, 10))
        Configuration.addTile(tile)
        Configuration.removeTile(tile.node)
        Assertions.assertFalse(tile.node in Configuration.tiles)
    }

    @Test
    fun `add robot`() {
        val robot = Robot(Node(5, -10))
        Configuration.addRobot(robot)
        Assertions.assertEquals(robot, Configuration.robots[robot.node])
    }

    @Test
    fun `remove robot`() {
        val robot = Robot(Node(-10, 5))
        Configuration.addRobot(robot)
        Configuration.removeRobot(robot.node)
        Assertions.assertFalse(robot.node in Configuration.robots)
    }

    @Test
    fun `add target`() {
        val node = Node(10, 5)
        Configuration.addTarget(node)
        Assertions.assertTrue(node in Configuration.targetNodes)
    }

    @Test
    fun `remove target`() {
        val node = Node(5, 10)
        Configuration.addTarget(node)
        Configuration.removeTarget(node)
        Assertions.assertFalse(node in Configuration.targetNodes)
    }

    @Test
    fun `clear configuration`() {
        Configuration.addRobot(Robot(Node(192, 663)))
        Configuration.addTile(Tile(Node(-55, -80)))
        Configuration.addTarget(Node(86, 591))
        Configuration.clear()
        Assertions.assertTrue(Configuration.robots.isEmpty())
        Assertions.assertTrue(Configuration.tiles.isEmpty())
        Assertions.assertTrue(Configuration.targetNodes.isEmpty())
    }

    @Test
    fun `undo nothing`() {
        Assertions.assertFalse(Configuration.undo())
    }

    @Test
    fun `redo nothing`() {
        Assertions.assertFalse(Configuration.redo())
    }

    @Test
    fun `undo step`() {
        val tile = Tile(Node(-10, -5))
        Configuration.addTile(tile, addUndoStep = true)
        Assertions.assertEquals(1, Configuration.undoSteps())
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertFalse(tile.node in Configuration.tiles)
        Assertions.assertEquals(0, Configuration.undoSteps())
        Assertions.assertEquals(1, Configuration.redoSteps())
    }

    @Test
    fun `correct undo order`() {
        val robot = Robot(Node(948, 611))
        val targetNode = Node(-707, -697)
        Configuration.addRobot(robot, addUndoStep = true)
        Configuration.addTarget(targetNode, addUndoStep = true)
        Assertions.assertEquals(2, Configuration.undoSteps())
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertFalse(targetNode in Configuration.targetNodes)
        Assertions.assertTrue(robot.node in Configuration.robots)
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertFalse(targetNode in Configuration.targetNodes)
        Assertions.assertFalse(robot.node in Configuration.robots)
        Assertions.assertEquals(0, Configuration.undoSteps())
    }

    @Test
    fun `redo step`() {
        val robot = Robot(Node(-5, -10))
        Configuration.addRobot(robot, addUndoStep = true)
        Assertions.assertEquals(1, Configuration.undoSteps())
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertFalse(robot.node in Configuration.robots)
        Assertions.assertTrue(Configuration.redo())
        Assertions.assertTrue(robot.node in Configuration.robots)
        Assertions.assertEquals(1, Configuration.undoSteps())
        Assertions.assertEquals(0, Configuration.redoSteps())
    }

    @Test
    fun `correct redo order`() {
        val targetNode = Node(807, -333)
        val tile = Tile(Node(-602, -784))
        Configuration.addTarget(targetNode, addUndoStep = true)
        Configuration.addTile(tile, addUndoStep = true)
        Assertions.assertEquals(2, Configuration.undoSteps())
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertEquals(0, Configuration.undoSteps())
        Assertions.assertEquals(2, Configuration.redoSteps())
        Assertions.assertTrue(Configuration.redo())
        Assertions.assertTrue(targetNode in Configuration.targetNodes)
        Assertions.assertFalse(tile.node in Configuration.tiles)
        Assertions.assertTrue(Configuration.redo())
        Assertions.assertTrue(targetNode in Configuration.targetNodes)
        Assertions.assertTrue(tile.node in Configuration.tiles)
        Assertions.assertEquals(2, Configuration.undoSteps())
        Assertions.assertEquals(0, Configuration.redoSteps())
    }

    @Test
    fun `no redo after changing configuration`() {
        Configuration.addTarget(Node(1024, 0), addUndoStep = true)
        Assertions.assertTrue(Configuration.undo())
        Assertions.assertEquals(1, Configuration.redoSteps())
        Configuration.addTile(Tile(Node(0, -4096)), addUndoStep = true)
        Assertions.assertEquals(0, Configuration.redoSteps())
    }

    @Test
    fun `get empty configuration json`() {
        val json = Configuration.getJson().replace("\\s+".toRegex(), "")
        val validResponses = arrayOf(
            "{\"robots\":{},\"targetNodes\":[],\"tiles\":{}}",
            "{\"targetNodes\":[],\"robots\":{},\"tiles\":{}}",
            "{\"targetNodes\":[],\"tiles\":{},\"robots\":{}}",
            "{\"robots\":{},\"tiles\":{},\"targetNodes\":[]}",
            "{\"tiles\":{},\"robots\":{},\"targetNodes\":[]}",
            "{\"tiles\":{},\"targetNodes\":[],\"robots\":{}}",
        )
        Assertions.assertTrue(json in validResponses)
    }

    @Test
    fun `load empty configuration from json string`() {
        val json = "{\"robots\" : {}, \"targetNodes\": [], \"tiles\": {}}"
        Configuration.loadConfiguration(json)
        Assertions.assertTrue(Configuration.robots.isEmpty())
        Assertions.assertTrue(Configuration.targetNodes.isEmpty())
        Assertions.assertTrue(Configuration.tiles.isEmpty())
    }

    @Test
    fun `load non-empty configuration from json string`() {
        val robotNode = Node(2048, -512)
        val robotOrientation = 1
        val targetNode = Node(-512, 2048)
        val tileNode = Node(0, 1024)

        val json = """
            {
                "robots" : {"$robotNode": {"orientation" : $robotOrientation, "node" : {"x" : ${robotNode.x}, "y" : ${robotNode.y}}}},
                "targetNodes" : [{"x" : ${targetNode.x}, "y" : ${targetNode.y}}],
                "tiles" : {"$tileNode": {"node" : {"x" : ${tileNode.x}, "y" : ${tileNode.y}}}}
            }
            """
        Configuration.loadConfiguration(json)
        Assertions.assertEquals(0, Configuration.redoSteps())

        Assertions.assertEquals(mutableSetOf(robotNode), Configuration.robots.keys)
        val robot = Configuration.robots[robotNode]!!
        Assertions.assertEquals(robotNode, robot.node)
        Assertions.assertEquals(robotOrientation, robot.orientation)

        Assertions.assertEquals(mutableSetOf(targetNode), Configuration.targetNodes)

        Assertions.assertEquals(mutableSetOf(tileNode), Configuration.tiles.keys)
        val tile = Configuration.tiles[tileNode]!!
        Assertions.assertEquals(tileNode, tile.node)
    }

    @Test
    fun `get configuration json and load it again`() {
        val robot = Robot(Node(-973, 306), orientation = 4)
        val tile = Tile(Node(-898, -162))
        val targetNode = Node(-265, 640)
        Configuration.addRobot(robot)
        Configuration.addTile(tile)
        Configuration.addTarget(targetNode)

        val json = Configuration.getJson()
        Configuration.clear()

        Configuration.loadConfiguration(json)
        Assertions.assertEquals(mutableSetOf(robot.node), Configuration.robots.keys)
        Assertions.assertEquals(robot.node, Configuration.robots[robot.node]!!.node)
        Assertions.assertEquals(robot.orientation, Configuration.robots[robot.node]!!.orientation)
        Assertions.assertEquals(mutableSetOf(tile.node), Configuration.tiles.keys)
        Assertions.assertEquals(tile.node, Configuration.tiles[tile.node]!!.node)
        Assertions.assertEquals(mutableSetOf(targetNode), Configuration.targetNodes)
    }

}
