package com.github.jfriemel.hybridsim.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.AlgorithmLoader
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.Scheduler
import ktx.app.KtxInputAdapter
import ktx.graphics.takeScreenshot
import ktx.log.logger
import java.nio.file.Paths
import java.time.LocalDateTime

private val logger = logger<InputHandler>()

class InputHandler(
    private val screen: SimScreen,
    private val menu: Menu,
) : KtxInputAdapter {
    private var mousePressed = false
    private var mouseX = 0
    private var mouseY = 0

    override fun keyDown(keycode: Int): Boolean {
        // Using the key codes from Input.Keys is pretty annoying since it assumes a standard QWERTY
        // layout
        when (keycode) {
            Input.Keys.UP -> screen.yMomentum = -1
            Input.Keys.LEFT -> screen.xMomentum = -1
            Input.Keys.DOWN -> screen.yMomentum = 1
            Input.Keys.RIGHT -> screen.xMomentum = 1
            Input.Keys.F11 -> toggleFullscreen()
            Input.Keys.SPACE -> {
                menu.untoggleToggleButtons()
                Scheduler.toggle()
            }

            Input.Keys.ESCAPE -> { // Emergency handbrake, basically stop everything
                Scheduler.stop()
                menu.untoggleToggleButtons()
                if (!menu.isActive()) {
                    menu.toggleActive()
                }
                if (Gdx.graphics.isFullscreen) {
                    toggleFullscreen()
                }
                screen.resetCamera()
            }

            Input.Keys.Y,
            Input.Keys.Z,
            -> {
                // Undo with Ctrl+Y or Ctrl+Z, redo with Ctrl+Shift+Y or Ctrl+Shift+Z
                // I strongly dislike this, but I see no alternative as I cannot access the user's
                // keyboard layout
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    Scheduler.stop()
                    if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                        Configuration.redo()
                    } else {
                        Configuration.undo()
                    }
                }
            }

            Input.Keys.F2 -> { // Take a screenshot
                try {
                    val time =
                        LocalDateTime
                            .now()
                            .toString()
                            .split(".")[0]
                            .replace(':', '-')
                    val path = Paths.get(System.getProperty("user.dir"), "screenshots", "$time.png")
                    takeScreenshot(FileHandle(path.toFile()))
                    logger.debug { "Screenshot: $path" }
                } catch (e: Exception) {
                    logger.error { "Screenshot failed!" }
                    logger.error { e.toString() }
                    logger.error { e.stackTraceToString() }
                }
            }
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        // Using the key codes from Input.Keys is pretty annoying since it assumes a standard QWERTY
        // layout
        when (keycode) {
            Input.Keys.UP,
            Input.Keys.DOWN,
            -> screen.yMomentum = 0

            Input.Keys.LEFT,
            Input.Keys.RIGHT,
            -> screen.xMomentum = 0
        }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        when (character) {
            '+' -> screen.zoom(-1f)
            '-' -> screen.zoom(1f)
            '0' -> screen.resetCamera()
            'f',
            'F',
            -> toggleFullscreen()

            'm',
            'M',
            -> menu.toggleActive()

            'l',
            'L',
            -> menu.loadConfiguration()

            's' -> menu.saveConfiguration(false)
            'S' -> menu.saveConfiguration(true)
            'a',
            'A',
            -> menu.loadAlgorithm()

            't',
            'T',
            -> menu.togglePutTiles()

            'r',
            'R',
            -> menu.togglePutRobots()

            'z',
            'Z',
            -> menu.toggleSelectTarget()

            'h',
            'H',
            -> menu.loadGenerator()

            'g',
            'G',
            -> menu.generateConfiguration()

            '<',
            '>',
            ->
                Configuration.targetNodes.addAll(
                    Configuration.tiles.keys,
                ) // Sometimes helpful for testing
        }
        return true
    }

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int,
    ): Boolean {
        val node = screen.screenCoordsToNodeCoords(screenX, screenY)
        when (button) {
            Input.Buttons.LEFT -> {
                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    menu.untoggleToggleButtons()
                    try {
                        Configuration.robots[node]?.triggerActivate()
                    } catch (e: Exception) {
                        logger.error { "Robot at $node crashed!" }
                        logger.error { e.toString() }
                        logger.error { e.stackTraceToString() }
                        Scheduler.stop()
                    }
                } else if (!menu.putTiles && !menu.putRobots && !menu.selectTarget) {
                    mousePressed = true
                    mouseX = screenX
                    mouseY = screenY
                } else {
                    touchDragged(screenX, screenY, pointer)
                }
            }

            Input.Buttons.RIGHT -> {
                if (
                    Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                ) { // Log coordinates, sometimes helpful
                    logger.debug { "Screen coordinates: ($screenX, $screenY)" }
                    logger.debug { "Node coordinates:   (${node.x}, ${node.y})" }
                } else {
                    touchDragged(screenX, screenY, pointer)
                }
            }
        }
        return true
    }

    override fun touchUp(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int,
    ): Boolean {
        mousePressed = false
        return true
    }

    override fun touchDragged(
        screenX: Int,
        screenY: Int,
        pointer: Int,
    ): Boolean {
        val node = screen.screenCoordsToNodeCoords(screenX, screenY)
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (mousePressed) {
                screen.move(mouseX - screenX, mouseY - screenY)
                mouseX = screenX
                mouseY = screenY
            } else if (menu.putTiles && node !in Configuration.tiles) {
                Configuration.addTile(Tile(node), addUndoStep = true)
                if (menu.selectTarget) { // Place tile and target in one step
                    Configuration.addTarget(node, addUndoStep = false)
                }
            } else if (menu.putRobots && node !in Configuration.robots) {
                val robot = AlgorithmLoader.getAlgorithmRobot(Robot(node))
                Configuration.addRobot(robot, addUndoStep = true)
            } else if (menu.selectTarget && node !in Configuration.targetNodes) {
                Configuration.addTarget(node, addUndoStep = true)
            }
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            if (menu.putTiles && node in Configuration.tiles) {
                Configuration.removeTile(node, addUndoStep = true)
                if (menu.selectTarget) { // Remove tile and target in one step
                    Configuration.removeTarget(node, addUndoStep = false)
                }
            } else if (menu.putRobots && node in Configuration.robots) {
                Configuration.removeRobot(node, addUndoStep = true)
            } else if (menu.selectTarget && node in Configuration.targetNodes) {
                Configuration.removeTarget(node, addUndoStep = true)
            }
        }
        return true
    }

    override fun mouseMoved(
        screenX: Int,
        screenY: Int,
    ): Boolean = false

    override fun scrolled(
        amountX: Float,
        amountY: Float,
    ): Boolean {
        screen.zoom(amountY, Gdx.input.x, Gdx.input.y)
        return true
    }

    private fun toggleFullscreen() {
        if (Gdx.graphics.isFullscreen) {
            // Scale down
            Gdx.graphics.setWindowedMode(Gdx.graphics.width * 8 / 10, Gdx.graphics.height * 8 / 10)
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }
    }
}
