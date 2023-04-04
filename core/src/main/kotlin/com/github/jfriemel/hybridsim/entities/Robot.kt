package com.github.jfriemel.hybridsim.entities

import com.badlogic.gdx.graphics.g2d.Sprite
import com.beust.klaxon.Json
import com.github.jfriemel.hybridsim.system.Configuration
import kotlin.random.Random

open class Robot(
    node: Node,
    val orientation: Int = Random.nextInt(0, 6),
    sprite: Sprite? = null,
    @Json(ignored = true) var carriesTile: Boolean = false,
    @Json(ignored = true) var numPebbles: Int = 0,
    @Json(ignored = true) val maxPebbles: Int = 2,
    @Json(ignored = true) var carrySprite: Sprite? = null,
) : Entity(node, sprite) {
    // Functions and constructor values cannot be private as they need to be accessible by the algorithm scripts
    // implementing custom Robots.

    @Json(ignored = true) val labels = intArrayOf(0, 1, 2, 3, 4, 5)

    /**
     * The code executed by the robot when it is activated.
     * The function is overriden by imported algorithms (.kts scripts).
     * As per the model, the algorithm needs to be convertible to an equivalent finite state automaton.
     * In other words, it needs to run in constant time and with constant space.
     * For further constraints, check out this paper: https://doi.org/10.1007/s11047-019-09774-2
     */
    open fun activate() {  // Default implementation does nothing
        return
    }

    /**
     * The function can be overriden by imported algorithms to indicate whether the robot is finished with executing
     * its algorithm.
     * When all robots are finished (i.e. every finished() call returns true), the Scheduler stops automatically.
     */
    open fun finished(): Boolean = false

    /** The robot tries to move to the node at the given [label]. Returns true if successful. */
    fun moveToLabel(label: Int): Boolean {
        if (nodeAtLabel(label) in Configuration.robots) {
            return false
        }
        Configuration.moveRobot(node, nodeAtLabel(label))
        node = nodeAtLabel(label)
        return true
    }

    /** Checks whether the robot is on top of a [Tile]. */
    fun isOnTile(): Boolean = node in Configuration.tiles

    /** @return The [Tile] at the robot's location or null if there is no tile. */
    fun tileBelow(): Tile? = Configuration.tiles[node]

    /** Tries to lift the [Tile] at the robot's current location. Returns true if successful. */
    fun liftTile(): Boolean {
        if (!isOnTile() || carriesTile || tileBelow()?.hasPebble() == true) {
            return false
        }
        Configuration.removeTile(node)
        carriesTile = true
        return true
    }

    /** Tries to place a [Tile] at the robot's current location. Returns true if successful. */
    fun placeTile(): Boolean {
        if (isOnTile() || !carriesTile) {
            return false
        }
        Configuration.addTile(Tile(node))
        carriesTile = false
        return true
    }

    /** @return True if there is a [Tile] at the robot's location, and it has a pebble. */
    fun tileHasPebble(): Boolean = tileBelow()?.hasPebble() == true

    /** Tries to put a pebble on the [Tile] at the robot's current location. Returns true if successful. */
    fun putPebble(): Boolean {
        if (!isOnTile() || tileHasPebble() || numPebbles == 0) {
            return false
        }
        tileBelow()?.addPebble()
        numPebbles--
        return true
    }

    /** Tries to take a pebble from the [Tile] at the robot's current location. Returns true if successful. */
    fun takePebble(): Boolean {
        if (!isOnTile() || !tileHasPebble() || numPebbles >= maxPebbles) {
            return false
        }
        tileBelow()?.removePebble()
        numPebbles++
        return true
    }

    /** @return True if the robot is on a target [Node]. */
    fun isOnTarget(): Boolean = node in Configuration.targetNodes

    /** @return True if the node at the [label] is a target [Node]. */
    fun labelIsTarget(label: Int): Boolean = nodeAtLabel(label) in Configuration.targetNodes

    /** @return True if the robot has a [Tile] neighbour that is on a target [Node]. */
    fun hasTargetTileNbr(): Boolean = labels.any { label -> hasTileAtLabel(label) && labelIsTarget(label) }

    /** @return A [Tile] neighbour that is on a target [Node] or null if there is no such neighbour. */
    fun targetTileNbrLabel(): Int? = labels.firstOrNull { label -> hasTileAtLabel(label) && labelIsTarget(label) }

    /** @return True if the robot has a [Tile] neighbour that is not on a target [Node]. */
    fun hasOverhangTileNbr(): Boolean = labels.any { label -> hasTileAtLabel(label) && !labelIsTarget(label) }

    /** @return A [Tile] neighbour that is not on a target [Node] or null if there is no such neighbour. */
    fun overhangTileNbrLabel(): Int? = labels.firstOrNull { label -> hasTileAtLabel(label) && !labelIsTarget(label) }

    /** @return The number of connected components of empty nodes adjacent to the robot. */
    fun numBoundaries(tileBoundaries: Boolean = false): Int {
        val boundaryCheck = if (tileBoundaries) {
            { label: Int -> hasTileAtLabel(label) }
        } else {
            { label: Int -> !hasTileAtLabel(label) }
        }
        val boundaryLabels = labels.filter { boundaryCheck(it) }
        var numBoundaries = 0
        boundaryLabels.forEach { label ->
            if ((label + 1).mod(6) !in boundaryLabels) {
                numBoundaries++
            }
        }
        return numBoundaries
    }

    /** Checks whether the robot has a neighbouring [Tile] at the given [label]. */
    fun hasTileAtLabel(label: Int): Boolean = nodeAtLabel(label) in Configuration.tiles

    /** @return The [Tile] at the robot's given [label] or null if there is no such tile. */
    fun tileAtLabel(label: Int): Tile? = Configuration.tiles[nodeAtLabel(label)]

    /** Checks whether the robot has a [Robot] neighbour at the given [label]. */
    fun hasRobotAtLabel(label: Int): Boolean = nodeAtLabel(label) in Configuration.robots

    /** @return The [Robot] neighbour at the given [label] of null if there is no such neighbour. */
    fun robotAtLabel(label: Int): Robot? = Configuration.robots[nodeAtLabel(label)]

    /** @return The [Node] at the given [label]. */
    @SuppressWarnings("WeakerAccess")
    protected fun nodeAtLabel(label: Int): Node = node.nodeInDir((orientation + label).mod(6))

}
