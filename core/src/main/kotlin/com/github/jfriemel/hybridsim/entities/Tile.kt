package com.github.jfriemel.hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.github.jfriemel.hybridsim.system.Configuration

// Typically, no more than one pebble is allowed per tile; but, you know, future-proofing
const val MAX_PEBBLES_TILE: Int = 1

class Tile(node: Node, sprite: Sprite? = null, private var numPebbles: Int = 0) : Entity(node, sprite) {
    // Constructor values cannot be private (even if the IDE claims otherwise) because they need to be accessed to save
    // configurations to JSON files.

    private var tileColor: Color? = null

    /**
     * Returns the tile's colour. The colour depends on whether the tile is located at a target node.
     * If no target nodes exist, all tiles are white.
     * If tileColor is not null, tileColor is returned.
     */
    override fun getColor(): Color {
        return if (tileColor != null) {
            tileColor!!
        } else if (Configuration.targetNodes.isEmpty()) {
            colorDefault
        } else if (node in Configuration.targetNodes) {
            colorTarget
        } else {
            colorNonTarget
        }
    }

    /** Sets tileColor to [color]. If tileColor is not null, the tile is drawn in that color. */
    @Suppress("Unused")
    fun setColor(color: Color?) {
        tileColor = color
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

    companion object {
        val colorDefault: Color = Color.LIGHT_GRAY
        val colorTarget: Color = Color.CYAN
        val colorNonTarget: Color = Color.CORAL
    }
}
