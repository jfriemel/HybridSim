package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.beust.klaxon.Json

abstract class Entity(var node: Node, @Json(ignored = true) var sprite: Sprite ?= null) {
    /**
     * @return The entity's colour.
     */
    open fun getColor(): Color {
        return Color.WHITE
    }
}
