package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import hybridsim.Configuration

const val MAX_PEBBLES: Int = 1

val TARGET_COLOR: Color = Color.CYAN
val DEFAULT_COLOR: Color = Color.CORAL

class Tile(node: Node, sprite: Sprite ?= null, var numPebbles: Int = 0): Entity(node, sprite) {
    // Constructor values cannot be private as they need to be accessed to save configurations.

    /**
     * The tile's colour depends on whether the tile is located at a target node.
     *
     * @return The tile's colour.
     */
    override fun getColor(): Color {
        return if (Configuration.targetNodes.contains(node)) {
            TARGET_COLOR
        } else {
            DEFAULT_COLOR
        }
    }

    /**
     * Adds a pebble to the tile if the tile can carry another pebble.
     *
     * @return True, if the pebble was added successfully. False, if the tile is already full.
     */
    fun addPebble(): Boolean {
        if (numPebbles >= MAX_PEBBLES) {
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
