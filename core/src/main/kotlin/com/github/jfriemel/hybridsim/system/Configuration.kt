package com.github.jfriemel.hybridsim.system

import com.beust.klaxon.Klaxon
import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.ui.AlgorithmLoader
import java.util.Deque
import java.util.LinkedList

data class TimeState(val tiles: MutableMap<Node, Tile>, val robots: MutableMap<Node, Robot>, val targetNodes: MutableSet<Node>)

const val MAX_UNDO_STATES = 1000

object Configuration {

    var tiles: MutableMap<Node, Tile> = HashMap()
    var robots: MutableMap<Node, Robot> = HashMap()
    var targetNodes: MutableSet<Node> = HashSet()

    private var undoQueue: Deque<TimeState> = LinkedList()
    private var redoQueue: Deque<TimeState> = LinkedList()

    init {
        loadConfiguration("{\"robots\" : {\"Node(x=0, y=0)\": {\"orientation\" : 4, \"node\" : {\"x\" : 0, \"y\" : 0}}}, \"targetNodes\" : [], \"tiles\" : {\"Node(x=0, y=-1)\": {\"node\" : {\"x\" : 0, \"y\" : -1}}, \"Node(x=0, y=0)\": {\"node\" : {\"x\" : 0, \"y\" : 0}}, \"Node(x=1, y=1)\": {\"node\" : {\"x\" : 1, \"y\" : 1}}, \"Node(x=0, y=1)\": {\"node\" : {\"x\" : 0, \"y\" : 1}}, \"Node(x=-1, y=1)\": {\"node\" : {\"x\" : -1, \"y\" : 1}}, \"Node(x=-1, y=0)\": {\"node\" : {\"x\" : -1, \"y\" : 0}}, \"Node(x=1, y=-1)\": {\"node\" : {\"x\" : 1, \"y\" : -1}}, \"Node(x=-1, y=-1)\": {\"node\" : {\"x\" : -1, \"y\" : -1}}}}\n")
    }

    /** Add a [tile] to the given [node]. */
    fun addTile(tile: Tile, node: Node) {
        addToQueue(undoQueue)
        tiles[node] = tile
    }

    /** Remove the tile at the given [node]. */
    fun removeTile(node: Node) {
        addToQueue(undoQueue)
        tiles.remove(node)
    }

    /** Add a [robot] to the given [node]. */
    fun addRobot(robot: Robot, node: Node) {
        addToQueue(undoQueue)
        robots[node] = robot
    }

    /** Remove the robot at the given [node]. */
    fun removeRobot(node: Node) {
        addToQueue(undoQueue)
        robots.remove(node)
    }

    /** Move the robot at [startNode] to [nextNode]. */
    fun moveRobot(startNode: Node, nextNode: Node) {
        addToQueue(undoQueue)
        val robot = robots.remove(startNode)
        if (robot != null) {
            robots[nextNode] = robot
        }
    }

    /** Add the given [node] to the target area. */
    fun addTarget(node: Node) {
        addToQueue(undoQueue)
        targetNodes.add(node)
    }

    /** Remove the given [node] from the target area. */
    fun removeTarget(node: Node) {
        addToQueue(undoQueue)
        targetNodes.remove(node)
    }

    /**
     * Undo/redo last operation that affected the configuration (default: undo).
     * Set [uq] to redoStates and [rq] to undoStates for redo.
     */
    fun undo(uq: Deque<TimeState> = undoQueue, rq: Deque<TimeState> = redoQueue): Boolean {
        if (uq.size == 0) {
            return false
        }
        addToQueue(rq)
        val undoState = uq.pollLast()
        tiles = undoState.tiles
        robots = undoState.robots
        targetNodes = undoState.targetNodes
        return true
    }

    /** Revert last undo. */
    fun redo(): Boolean = undo(redoQueue, undoQueue)

    /** Take a [json] string and load the configuration from that string. */
    fun loadConfiguration(json: String) {
        Scheduler.stop()
        Klaxon().parse<Configuration>(json = json)

//      Klaxon converts the keys of Maps to Strings. To get our Node keys back, we create temporary Maps, fill them
//      with the values parsed by Klaxon and then replace the Klaxon Maps with our temporary Maps.
//      There is probably a much cleaner way to do this (with Klaxon Converters), but I don't really care right now.
        tiles = tiles.values.associateBy { it.node }.toMutableMap()
        robots = robots.values.associateBy { it.node }.toMutableMap()
        robots.values.forEach { AlgorithmLoader.replaceRobot(it.node) }
        targetNodes = targetNodes.toMutableSet()

        clearUndoQueues()
    }

    /** Convert the current configuration string to a JSON string that can be saved to a file. */
    fun getJson(): String {
        return Klaxon().toJsonString(Configuration)
    }

    /** To avoid unexpected behaviour, the undo/redo queues can be cleared when loading configurations/algorithms. */
    fun clearUndoQueues() {
        undoQueue.clear()
        redoQueue.clear()
    }

    /** Add the current state of the configuration to the given undo/redo [queue]. */
    private fun addToQueue(queue: Deque<TimeState>) {
        if (queue.size == MAX_UNDO_STATES) {  // Keep memory consumption in check
            queue.removeFirst()
        }
        // Deep copies of robots and tiles:
        val queueTiles = tiles.mapValues { entry -> entry.value.clone() as Tile }.toMutableMap()
        val queueRobots = robots.mapValues { entry -> entry.value.clone() as Robot }.toMutableMap()
        queue.add(TimeState(queueTiles, queueRobots, targetNodes.toMutableSet()))
    }

}
