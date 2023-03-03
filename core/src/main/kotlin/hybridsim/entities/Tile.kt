package hybridsim.entities

import com.badlogic.gdx.graphics.g2d.Sprite

class Tile(node: Node, sprite: Sprite ?= null): Entity(node, sprite) {

    private var numPebbles = 0

    fun addPebble() {
        numPebbles++
    }

    fun removePebble() {
        numPebbles--
    }

    fun hasPebble(): Boolean {
        return numPebbles > 0
    }
}
