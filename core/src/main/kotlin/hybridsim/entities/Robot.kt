package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.beust.klaxon.Json
import hybridsim.Configuration
import kotlin.random.Random

const val MAX_PEBBLES_ROBOT: Int = 2

open class Robot(val orientation: Int, node: Node, sprite: Sprite ?= null, var carriesTile: Boolean = false,
                 var numPebbles: Int = 0, @Json(ignored = true) var carrySprite: Sprite ?= null): Entity(node, sprite) {
    // Constructor values cannot be private (even if the IDE claims otherwise) because they need to be accessed to save
    // configurations to JSON files.

    private var color = Color.WHITE

    override fun getColor(): Color {
        return color
    }

    /**
     * The code executed by the robot when it is activated.
     * As per the model, the algorithm needs to be convertible to an equivalent finite state automaton.
     * In other words, it needs to run in constant time and with constant space.
     * For further constraints, check out this paper: https://doi.org/10.1007/s11047-019-09774-2
     */
    fun activate() {  // Example implementation for testing
        color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
        val label = Random.nextInt(0, 6)
        if (isOnTile() && !carriesTile && !tileHasPebble() && Random.nextBoolean()) {
            liftTile()
        } else if (!isOnTile() && carriesTile && Random.nextBoolean()) {
            placeTile()
        } else if (isOnTile() && !tileHasPebble() && numPebbles > 0 && Random.nextBoolean()) {
            putPebble()
        } else if (isOnTile() && tileHasPebble() && numPebbles < MAX_PEBBLES_ROBOT && Random.nextBoolean()) {
            takePebble()
        } else if ((isOnTile() || hasTileAtLabel(label)) && !hasRobotAtLabel(label)) {
            moveToLabel(label)
        }
    }

    /** The robot moves to the node at the given [label]. */
    private fun moveToLabel(label: Int) {
        Configuration.robots.remove(node)
        node = nodeAtLabel(label)
        Configuration.robots[node] = this
    }

    /** Checks whether the robot is on top of a tile. */
    private fun isOnTile(): Boolean {
        return Configuration.tiles.contains(node)
    }

    /** Returns the tile at the robot's location or null if there is no tile. */
    private fun tileBelow(): Tile? {
        return Configuration.tiles[node]
    }

    /** Tries to lift the tile at the robot's current location. Returns true if successful. */
    private fun liftTile(): Boolean {
        if (!isOnTile() || carriesTile || tileBelow()?.hasPebble() == true) {
            return false
        }
        Configuration.tiles.remove(node)
        carriesTile = true
        return true
    }

    /** Tries to place a tile at the robot's current location. Returns true if successful. */
    private fun placeTile(): Boolean {
        if (isOnTile() || !carriesTile) {
            return false
        }
        Configuration.tiles[node] = Tile(node)
        carriesTile = false
        return true
    }

    /** Returns true if there is a tile at the robot's location, and it has a pebble. */
    private fun tileHasPebble(): Boolean {
        return tileBelow()?.hasPebble() == true
    }

    /** Tries to put a pebble on the tile at the robot's current location. Returns true if successful. */
    private fun putPebble(): Boolean {
        if (!isOnTile() || tileHasPebble() || numPebbles == 0) {
            return false
        }
        tileBelow()?.addPebble()
        numPebbles--
        return true
    }

    /** Tries to take a pebble from the tile at the robot's current location. Returns true if successful. */
    private fun takePebble(): Boolean {
        if (!isOnTile() || !tileHasPebble() || numPebbles >= MAX_PEBBLES_ROBOT) {
            return false
        }
        tileBelow()?.removePebble()
        numPebbles++
        return true
    }

    /** Checks whether the robot has a neighbouring tile at the given [label]. */
    private fun hasTileAtLabel(label: Int): Boolean {
        return Configuration.tiles.contains(nodeAtLabel(label))
    }

    /** Returns the tile at the robot's given [label] or null if there is no such tile. */
    private fun tileAtLabel(label: Int): Tile? {
        return Configuration.tiles[nodeAtLabel(label)]
    }

    /** Checks whether the robot has a robot neighbour at the given [label]. */
    private fun hasRobotAtLabel(label: Int): Boolean {
        return Configuration.robots.contains(nodeAtLabel(label))
    }

    /** Returns the robot neighbour at the given [label] of null if there is no such neighbour. */
    private fun robotAtLabel(label: Int): Robot? {
        return Configuration.robots[nodeAtLabel(label)]
    }

    /** Returns the node at the given [label]. */
    private fun nodeAtLabel(label: Int): Node {
        return node.nodeInDir((orientation + label).mod(6))
    }
}
