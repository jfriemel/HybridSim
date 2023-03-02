package hybridsim

import com.badlogic.gdx.graphics.g2d.Sprite

class Tile(var x: Int, var y: Int, var sprite: Sprite ?= null) {
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
