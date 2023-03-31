package com.github.jfriemel.hybridsim.system

import com.beust.klaxon.Klaxon
import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.ui.AlgorithmLoader
import java.util.Deque
import java.util.LinkedList

data class TimeState(
    val tiles: MutableMap<Node, Tile>,
    val robots: MutableMap<Node, Robot>,
    val targetNodes: MutableSet<Node>,
)

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

    /** Add a [tile] to the configuration at the [tile]'s node. */
    fun addTile(tile: Tile) {
        addToUndoQueue()
        tiles[tile.node] = tile
    }

    /** Remove the [Tile] at the given [node] if it exists. */
    fun removeTile(node: Node) {
        addToUndoQueue()
        tiles.remove(node)
    }

    /** Add a [robot] to the configuration at the [robot]'s node. */
    fun addRobot(robot: Robot) {
        addToUndoQueue()
        robots[robot.node] = robot
    }

    /** Remove the [Robot] at the given [node] if it exists. */
    fun removeRobot(node: Node) {
        addToUndoQueue()
        robots.remove(node)
    }

    /** Move the [Robot] at [startNode] to [nextNode] if it exists. */
    fun moveRobot(startNode: Node, nextNode: Node) {
        addToUndoQueue()
        val robot = robots.remove(startNode) ?: return
        robots[nextNode] = robot
    }

    /** Add the given [node] to the target area. */
    fun addTarget(node: Node) {
        addToUndoQueue()
        targetNodes.add(node)
    }

    /** Remove the given [node] from the target area. */
    fun removeTarget(node: Node) {
        addToUndoQueue()
        targetNodes.remove(node)
    }

    /** Undo the last operation that affected the [Configuration]. Returns true if undo was successful. */
    fun undo() = undo(undoQueue, redoQueue)

    /** Revert last undo. Returns true if redo was successful. */
    fun redo(): Boolean = undo(redoQueue, undoQueue)

    /** @return Number of steps that can be undone. */
    fun undoSteps(): Int = undoQueue.size

    /** @return Number of steps that can be redone. */
    fun redoSteps(): Int = redoQueue.size

    /**
     * Undo/redo the last operation that affected the [Configuration].
     * For undo: [uq] := [undoQueue], [rq] := [redoQueue]
     * For redo: [uq] := [redoQueue], [rq] := [undoQueue]
     * @return True if undo/redo was successful.
     */
    private fun undo(uq: Deque<TimeState>, rq: Deque<TimeState>): Boolean {
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

    /** Take a [json] string and load the [Configuration] from that string. */
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

    /** Convert the current [Configuration] string to a JSON string that can be saved to a file. */
    fun getJson(): String = Klaxon().toJsonString(Configuration)

    /** To avoid unexpected behaviour, the undo/redo queues can be cleared when loading [Configuration]s/algorithms. */
    fun clearUndoQueues() {
        undoQueue.clear()
        redoQueue.clear()
    }

    /** Add the current state of the [Configuration] to the [undoQueue], clear the [redoQueue]. */
    private fun addToUndoQueue() {
        addToQueue(undoQueue)
        redoQueue.clear()
    }

    /** Add the current state of the [Configuration] to the given undo/redo [queue]. */
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
