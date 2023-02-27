package hybridsim

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor

class InputHandler(private val screen: SimScreen) : InputProcessor {
    override fun keyDown(keycode: Int): Boolean {
        // Using the key codes from Input.Keys is pretty annoying since it assumes a standard QWERTY layout
        when (keycode) {
            Input.Keys.W, Input.Keys.UP -> screen.yMomentum = -1
            Input.Keys.A, Input.Keys.LEFT -> screen.xMomentum = -1
            Input.Keys.S, Input.Keys.DOWN -> screen.yMomentum = 1
            Input.Keys.D, Input.Keys.RIGHT -> screen.xMomentum = 1
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        // Using the key codes from Input.Keys is pretty annoying since it assumes a standard QWERTY layout
        when (keycode) {
            Input.Keys.W, Input.Keys.UP, Input.Keys.S, Input.Keys.DOWN -> screen.yMomentum = 0
            Input.Keys.A, Input.Keys.LEFT, Input.Keys.D, Input.Keys.RIGHT -> screen.xMomentum = 0
        }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        when (character) {
            '+' -> screen.zoom(-1f)
            '-' -> screen.zoom(1f)
        }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        screen.zoom(amountY, Gdx.input.x, Gdx.input.y)
        return true
    }
}
