package com.github.jfriemel.hybridsim

import com.badlogic.gdx.graphics.Color
import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.Configuration
import org.junit.jupiter.api.Test

class TestTile {

    @Test
    fun `has pebble`() {
        val tile = Tile(Node(1, -1), numPebbles = 1)
        assert(tile.hasPebble())
    }

    @Test
    fun `has no pebble`() {
        val tile = Tile(Node(-1, 1), numPebbles = 0)
        assert(!tile.hasPebble())
    }

    @Test
    fun `can add pebble`() {
        val tile = Tile(Node(3, 5), numPebbles = 0)
        assert(tile.addPebble())
        assert(tile.hasPebble())
    }

    @Test
    fun `can remove pebble`() {
        val tile = Tile(Node(-20, 30), numPebbles = 1)
        assert(tile.removePebble())
        assert(!tile.hasPebble())
    }

    @Test
    fun `cannot add pebble if tile has one`() {
        val tile = Tile(Node(0, 0), numPebbles = 1)
        assert(!tile.addPebble())
        assert(tile.hasPebble())
    }

    @Test
    fun `cannot remove pebble if tile has none`() {
        val tile = Tile(Node(1, 7), numPebbles = 0)
        assert(!tile.removePebble())
        assert(!tile.hasPebble())
    }

    @Test
    fun `has default colors`() {
        val tile = Tile(Node(Int.MAX_VALUE, Int.MIN_VALUE))
        Configuration.addTile(tile)
        assert(tile.getColor() == Tile.colorDefault)
        Configuration.addTarget(tile.node)
        assert(tile.getColor() == Tile.colorTarget)
        Configuration.removeTarget(tile.node)
        Configuration.addTarget(Node(0, 0))
        assert(tile.getColor() == Tile.colorNonTarget)
    }

    @Test
    fun `can set color`() {
        val tile = Tile(Node(Int.MIN_VALUE, Int.MAX_VALUE))
        tile.setColor(Color.CHARTREUSE)
        assert(tile.getColor() == Color.CHARTREUSE)
    }

}
