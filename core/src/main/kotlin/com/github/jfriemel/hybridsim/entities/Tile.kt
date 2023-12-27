package com.github.jfriemel.hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.github.jfriemel.hybridsim.system.Configuration

// Typically, no more than one pebble is allowed per tile; but, you know, future-proofing
const val MAX_PEBBLES_TILE: Int = 1

class Tile(node: Node, sprite: Sprite? = null, private var numPebbles: Int = 0) :
    Entity(node, sprite) {
    private var tileColor: Color? = null

    /**
     * Returns the tile's color. If [tileColor] is not null, [tileColor] is returned. Otherwise, if
     * no target nodes exist, [colorDefault] is returned. If the tile is at a target node,
     * [colorTarget] is returned. If not, [colorOverhang] is returned.
     */
    override fun getColor(): Color {
        return if (tileColor != null) {
            tileColor!!
        } else if (Configuration.targetNodes.isEmpty()) {
            colorDefault
        } else if (node in Configuration.targetNodes) {
            colorTarget
        } else {
            colorOverhang
        }
    }

    /** Sets [tileColor] to [color]. If [tileColor] is not null, the tile is drawn in that color. */
    fun setColor(color: Color?) {
        tileColor = color
    }

    /**
     * Adds a pebble to the tile if the tile can hold another pebble. Returns true if successful.
     */
    fun addPebble(): Boolean {
        if (numPebbles >= MAX_PEBBLES_TILE) {
            return false
        }
        numPebbles++
        return true
    }

    /**
     * Removes a pebble from the tile if the tile is holding a pebble. Returns true if successful.
     */
    fun removePebble(): Boolean {
        if (numPebbles <= 0) {
            return false
        }
        numPebbles--
        return true
    }

    /** Checks whether the tile has a pebble. */
    fun hasPebble(): Boolean = numPebbles > 0

    companion object {
        val colorDefault: Color = Color.LIGHT_GRAY
        val colorTarget: Color = Color.ROYAL
        val colorOverhang: Color = Color.CORAL
    }
}
