package com.github.jfriemel.hybridsim.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.Scheduler
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import ktx.app.KtxInputAdapter
import ktx.log.logger
import kotlin.random.Random

private val logger = logger<InputHandler>()

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
            Input.Keys.SPACE -> {
                menu.deactivateToggleButtons()
                Scheduler.toggle()
            }
            Input.Keys.ESCAPE -> {  // Emergency handbrake, basically stop everything
                Scheduler.stop()
                menu.deactivateToggleButtons()
                menu.active = true
                if (Gdx.graphics.isFullscreen) {
                    Gdx.graphics.setWindowedMode(1024, 768)
                }
            }
            Input.Keys.Y, Input.Keys.Z -> {
                // Undo with Ctrl+Y or Ctrl+Z, redo with Ctrl+Shift+Y or Ctrl+Shift+Z
                // I strongly dislike this, but I see no alternative as I cannot access the user's keyboard layout
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    Scheduler.stop()
                    if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                        Configuration.redo()
                    } else {
                        Configuration.undo()
                    }
                }
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
            'x', 'X' -> menu.loadAlgorithm()
            't', 'T' -> menu.togglePutTiles()
            'r', 'R' -> menu.togglePutRobots()
            'z', 'Z' -> menu.toggleSelectTarget()
        }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val node = screen.screenCoordsToNodeCoords(screenX, screenY)
        when (button) {
            Input.Buttons.LEFT -> {
                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    Configuration.robots[node]?.activate()
                } else if (menu.putTiles && node !in Configuration.tiles) {
                    Configuration.addTile(Tile(node), node)
                } else if (menu.putRobots && node !in Configuration.robots) {
                    val robot = AlgorithmLoader.getAlgorithmRobot(Robot(Random.nextInt(0, 6), node))
                    Configuration.addRobot(robot, node)
                } else if (menu.selectTarget && node !in Configuration.targetNodes) {
                    Configuration.addTarget(node)
                } else {
                    mouseX = screenX
                    mouseY = screenY
                    mousePressed = true
                }
            }
            Input.Buttons.RIGHT -> {
                if (menu.putTiles && node in Configuration.tiles) {
                    Configuration.removeTile(node)
                } else if (menu.putRobots && node in Configuration.robots) {
                    Configuration.removeRobot(node)
                } else if (menu.selectTarget && node in Configuration.targetNodes) {
                    Configuration.removeTarget(node)
                } else if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {  // Log coordinates, sometimes helpful
                    logger.debug { "Screen coordinates:     ($screenX, $screenY)" }
                    logger.debug { "Node coordinates:       (${node.x}, ${node.y})" }
                    val sciCoords = node.scientificCoordinates()
                    logger.debug { "Scientific coordinates: (${sciCoords.first}, ${sciCoords.second})" }
                }
            }
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        mousePressed = false
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val node = screen.screenCoordsToNodeCoords(screenX, screenY)
        if (mousePressed) {
            screen.move(mouseX - screenX, mouseY - screenY)
            mouseX = screenX
            mouseY = screenY
        } else if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            Configuration.robots[node]?.activate()
        } else if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (menu.putTiles && node !in Configuration.tiles) {
                Configuration.addTile(Tile(node), node)
            } else if (menu.putRobots && node !in Configuration.robots) {
                val robot = AlgorithmLoader.getAlgorithmRobot(Robot(Random.nextInt(0, 6), node))
                Configuration.addRobot(robot, node)
            } else if (menu.selectTarget && node !in Configuration.targetNodes) {
                Configuration.addTarget(node)
            }
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            if (menu.putTiles && node in Configuration.tiles) {
                Configuration.removeTile(node)
            } else if (menu.putRobots && node in Configuration.robots) {
                Configuration.removeRobot(node)
            } else if (menu.selectTarget && node in Configuration.targetNodes) {
                Configuration.removeTarget(node)
            }
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