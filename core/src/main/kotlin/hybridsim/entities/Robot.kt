package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import hybridsim.Configuration
import kotlin.random.Random

class Robot(val orientation: Int, node: Node, sprite: Sprite ?= null): Entity(node, sprite) {

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
        if (isOnTile() || hasTileAtLabel(label)) {
            moveToLabel(label)
        }
    }

    /**
     * The robot moves to the node at the given label.
     *
     * @param label Port label.
     */
    private fun moveToLabel(label: Int) {
        node = nodeAtLabel(label)
    }

    /**
     * @return True, if the robot is on top of a tile. False, otherwise.
     */
    private fun isOnTile(): Boolean {
        return Configuration.tiles.contains(node)
    }

    /**
     * @param label Port label.
     * @return True, if the robot has a neighbouring tile at the given label. False, otherwise.
     */
    private fun hasTileAtLabel(label: Int): Boolean {
        return Configuration.tiles.contains(nodeAtLabel(label))
    }

    /**
     * @param label Port label.
     * @return The neighbouring tile at the given label or null if there is no such tile.
     */
    private fun tileAtLabel(label: Int): Tile? {
        return Configuration.tiles[nodeAtLabel(label)]
    }

    /**
     * @param label Port label.
     * @return True, if the robot has a robot neighbour at the given label. False, otherwise.
     */
    private fun hasRobotAtLabel(label: Int): Boolean {
        return Configuration.robots.contains(nodeAtLabel(label))
    }

    /**
     * @param label Port label.
     * @return The robot neighbour at the given label of null if there is no such neighbour.
     */
    private fun robotAtLabel(label: Int): Robot? {
        return Configuration.robots[nodeAtLabel(label)]
    }

    /**
     * @param label Port label.
     * @return The node at the given label.
     */
    private fun nodeAtLabel(label: Int): Node {
        return node.nodeInDir((orientation + label).mod(6))
    }
}
