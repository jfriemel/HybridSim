package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import hybridsim.Configuration

class Tile(node: Node, sprite: Sprite ?= null, private var numPebbles: Int = 0, private var maxPebbles: Int = 1):
    Entity(node, sprite) {

    /**
     * The tile's colour depends on whether the tile is located at a target node.
     *
     * @return The tile's colour.
     */
    override fun getColor(): Color {
        return if (Configuration.targetNodes.contains(node)) {
            Color.CYAN
        } else {
            Color.CORAL
        }
    }

    /**
     * Adds a pebble to the tile if the tile can carry another pebble.
     *
     * @return True, if the pebble was added successfully. False, if the tile is already full.
     */
    fun addPebble(): Boolean {
        if (numPebbles >= maxPebbles) {
            return false
        }
        numPebbles++
        return true
    }

    /**
     * Removes a pebble from the tile is the tile still has a pebble.
     *
     * @return True, if the pebble was removed successfully. False, if the tile is already empty.
     */
    fun removePebble(): Boolean {
        if (numPebbles <= 0) {
            return false
        }
        numPebbles--
        return true
    }

    /**
     * @return True, if the tile has at least one pebble. False, otherwise.
     */
    fun hasPebble(): Boolean {
        return numPebbles > 0
    }
}
