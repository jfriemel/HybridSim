package hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite

abstract class Entity(var node: Node, var sprite: Sprite ?= null, var color: Color = Color.WHITE)
