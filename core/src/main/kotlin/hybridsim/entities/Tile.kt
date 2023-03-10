package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import hybridsim.Configuration

// Typically, no more than one pebble is allowed per tile. But, you know, future-proofing.
const val MAX_PEBBLES_TILE: Int = 1

val DEFAULT_COLOR: Color = Color.WHITE
val TARGET_COLOR: Color = Color.CYAN
val NON_TARGET_COLOR: Color = Color.CORAL

class Tile(node: Node, sprite: Sprite ?= null, var numPebbles: Int = 0): Entity(node, sprite) {
    // Constructor values cannot be private (even if the IDE claims otherwise) because they need to be accessed to save
    // configurations to JSON files.

    /**
     * Returns the tile's colour. The colour depends on whether the tile is located at a target node.
     * If no target nodes exist, all tiles are white.
     */
    override fun getColor(): Color {
        return if (Configuration.targetNodes.isEmpty()) {
            DEFAULT_COLOR
        } else if (Configuration.targetNodes.contains(node)) {
            TARGET_COLOR
        } else {
            NON_TARGET_COLOR
        }
    }

    /** Adds a pebble to the tile if the tile can carry another pebble. Returns true if successful. */
    fun addPebble(): Boolean {
        if (numPebbles >= MAX_PEBBLES_TILE) {
            return false
        }
        numPebbles++
        return true
    }

    /** Removes a pebble from the tile is the tile still has a pebble. Returns true if successful. */
    fun removePebble(): Boolean {
        if (numPebbles <= 0) {
            return false
        }
        numPebbles--
        return true
    }

    /** Checks whether the tile has a pebble. */
    fun hasPebble(): Boolean {
        return numPebbles > 0
    }
}
