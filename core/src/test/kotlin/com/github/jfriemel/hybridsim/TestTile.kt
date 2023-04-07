package com.github.jfriemel.hybridsim

import com.badlogic.gdx.graphics.Color
import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/** Test [Tile] functionality. */
class TestTile {

    @Test
    fun `has pebble`() {
        val tile = Tile(Node(1, -1), numPebbles = 1)
        Assertions.assertTrue(tile.hasPebble())
    }

    @Test
    fun `has no pebble`() {
        val tile = Tile(Node(-1, 1), numPebbles = 0)
        Assertions.assertFalse(tile.hasPebble())
    }

    @Test
    fun `can add pebble`() {
        val tile = Tile(Node(3, 5), numPebbles = 0)
        Assertions.assertTrue(tile.addPebble())
        Assertions.assertTrue(tile.hasPebble())
    }

    @Test
    fun `can remove pebble`() {
        val tile = Tile(Node(-20, 30), numPebbles = 1)
        Assertions.assertTrue(tile.removePebble())
        Assertions.assertFalse(tile.hasPebble())
    }

    @Test
    fun `cannot add pebble if tile has one`() {
        val tile = Tile(Node(0, 0), numPebbles = 1)
        Assertions.assertFalse(tile.addPebble())
        Assertions.assertTrue(tile.hasPebble())
    }

    @Test
    fun `cannot remove pebble if tile has none`() {
        val tile = Tile(Node(1, 7), numPebbles = 0)
        Assertions.assertFalse(tile.removePebble())
        Assertions.assertFalse(tile.hasPebble())
    }

    @Test
    fun `has default colours`() {
        Configuration.clear()
        val tile = Tile(Node(Int.MAX_VALUE, Int.MIN_VALUE))
        Configuration.addTile(tile)
        Assertions.assertEquals(Tile.colorDefault, tile.getColor())
        Configuration.addTarget(tile.node)
        Assertions.assertEquals(Tile.colorTarget, tile.getColor())
        Configuration.removeTarget(tile.node)
        Configuration.addTarget(Node(0, 0))
        Assertions.assertEquals(Tile.colorOverhang, tile.getColor())
    }

    @Test
    fun `can set colour`() {
        val tile = Tile(Node(Int.MIN_VALUE, Int.MAX_VALUE))
        tile.setColor(Color.CHARTREUSE)
        Assertions.assertEquals(Color.CHARTREUSE, tile.getColor())
    }

}
