package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import hybridsim.Configuration
import kotlin.random.Random

class Robot(private val orientation: Int, node: Node, sprite: Sprite ?= null): Entity(node, sprite) {
    fun activate() {  // Example implementation for testing
        color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
        val dir = Random.nextInt(0, 6)
        if (isOnTile() || hasTileInDir(dir)) {
            move(dir)
        }
    }

    private fun move(dir: Int) {
        node = nodeInDir(dir)
    }

    private fun isOnTile(): Boolean {
        return Configuration.tiles.contains(node)
    }

    private fun hasTileInDir(dir: Int): Boolean {
        return Configuration.tiles.contains(nodeInDir(dir))
    }

    private fun tileInDir(dir: Int): Tile? {
        return Configuration.tiles[nodeInDir(dir)]
    }

    private fun hasRobotInDir(dir: Int): Boolean {
        return Configuration.robots.contains(nodeInDir(dir))
    }

    private fun robotInDir(dir: Int): Robot? {
        return Configuration.robots[nodeInDir(dir)]
    }

    private fun nodeInDir(dir: Int): Node {
        return node.nodeInDir((orientation + dir).mod(6))
    }
}
