package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import hybridsim.Configuration
import kotlin.random.Random

class Robot(private val orientation: Int, node: Node, sprite: Sprite ?= null): Entity(node, sprite) {

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

    private fun moveToLabel(label: Int) {
        node = nodeAtLabel(label)
    }

    private fun isOnTile(): Boolean {
        return Configuration.tiles.contains(node)
    }

    private fun hasTileAtLabel(label: Int): Boolean {
        return Configuration.tiles.contains(nodeAtLabel(label))
    }

    private fun tileAtLabel(label: Int): Tile? {
        return Configuration.tiles[nodeAtLabel(label)]
    }

    private fun hasRobotAtLabel(label: Int): Boolean {
        return Configuration.robots.contains(nodeAtLabel(label))
    }

    private fun robotAtLabel(label: Int): Robot? {
        return Configuration.robots[nodeAtLabel(label)]
    }

    private fun nodeAtLabel(label: Int): Node {
        return node.nodeInDir((orientation + label).mod(6))
    }
}
