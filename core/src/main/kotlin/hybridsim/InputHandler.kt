package hybridsim

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import hybridsim.entities.Node
import hybridsim.ui.Menu
import hybridsim.ui.SimScreen
import ktx.app.KtxInputAdapter

class InputHandler(private val screen: SimScreen, private val menu: Menu) : KtxInputAdapter {
    private var mousePressed = false
    private var mouseX = 0
    private var mouseY = 0

    override fun keyDown(keycode: Int): Boolean {
        // Using the key codes from Input.Keys is pretty annoying since it assumes a standard QWERTY layout
        when (keycode) {
            Input.Keys.UP -> screen.yMomentum = -1
            Input.Keys.LEFT -> screen.xMomentum = -1
            Input.Keys.DOWN -> screen.yMomentum = 1
            Input.Keys.RIGHT -> screen.xMomentum = 1
            Input.Keys.F11 -> toggleFullscreen()
            Input.Keys.SPACE -> Scheduler.toggle()
            Input.Keys.ESCAPE -> {  // Emergency handbrake, basically stop everything
                if (Gdx.graphics.isFullscreen) {
                    Gdx.graphics.setWindowedMode(1024, 768)
                }
                Scheduler.stop()
            }
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        // Using the key codes from Input.Keys is pretty annoying since it assumes a standard QWERTY layout
        when (keycode) {
            Input.Keys.UP, Input.Keys.DOWN -> screen.yMomentum = 0
            Input.Keys.LEFT, Input.Keys.RIGHT -> screen.xMomentum = 0
        }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        when (character) {
            '+' -> screen.zoom(-1f)
            '-' -> screen.zoom(1f)
            'f', 'F' -> toggleFullscreen()
            'm', 'M' -> menu.active = !menu.active
            'l', 'L' -> menu.loadConfiguration()
            'k', 'K' -> menu.saveConfiguration()
        }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> {
                mouseX = screenX
                mouseY = screenY
                mousePressed = true
            }
            Input.Buttons.RIGHT -> {  // Only for testing, will be removed later
                println("($screenX, $screenY)")
                val node = screen.screenCoordsToNodeCoords(screenX, screenY)
                println(node)
                val sciCoords = node.scientificCoordinates()
                println(sciCoords)
                println(Node.sciCoordsToNode(sciCoords.first, sciCoords.second))
                println(screen.nodeCoordsToScreenCoords(node.x, node.y))
                print("\n")
            }
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        mousePressed = false
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (mousePressed) {
            screen.move(mouseX - screenX, mouseY - screenY)
            mouseX = screenX
            mouseY = screenY
        }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        screen.zoom(amountY, Gdx.input.x, Gdx.input.y)
        return true
    }

    private fun toggleFullscreen() {
        if (Gdx.graphics.isFullscreen) {
            Gdx.graphics.setWindowedMode(1024, 768)
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }
    }
}
