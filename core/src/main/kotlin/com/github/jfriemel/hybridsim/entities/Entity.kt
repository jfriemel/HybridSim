package com.github.jfriemel.hybridsim.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.beust.klaxon.Json

abstract class Entity(var node: Node, @Json(ignored = true) var sprite: Sprite ?= null) : Cloneable {
    /** Called by the screen to draw the entity in the correct colour. */
    open fun getColor(): Color {
        return Color.WHITE
    }

    public override fun clone(): Any {
        return super.clone()
    }
}