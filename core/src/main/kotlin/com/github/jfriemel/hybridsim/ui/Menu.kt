package com.github.jfriemel.hybridsim.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.Main
import com.github.jfriemel.hybridsim.system.Scheduler
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton
import java.io.File
import java.lang.Exception
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

private val logger = ktx.log.logger<Main>()

private const val BUTTON_WIDTH = 190f

class Menu(batch: Batch) {

    // Indicates whether the menu is shown on the screen
    var active = true

    // When active, tiles/robots/target nodes can be added/removed by mouse clicks
    var putTiles = false
    var putRobots = false
    var selectTarget = false

    // All menu buttons
    private var buttonLoadConfig: TextButton
    private var buttonSaveConfig: TextButton
    private var buttonLoadAlgorithm: TextButton
    private var buttonPutTiles: TextButton
    private var buttonPutRobots: TextButton
    private var buttonSelectTarget: TextButton

    // File extension filters for the files used by the simulator
    private val jsonFilter = FileNameExtensionFilter("HybridSim configuration JSON files", "json")
    private val algoFilter = FileNameExtensionFilter("HybridSim algorithm scripts", "kts")

    val menuStage = Stage(ScreenViewport(OrthographicCamera()), batch).apply {
        actors {
            table {
                setFillParent(true)
                defaults().pad(2f)
                add(label("Menu (M)")).actor.color = Color.BLACK
                row()
                buttonLoadConfig = add(textButton("Load Configuration (L)")).width(BUTTON_WIDTH).actor
                row()
                buttonSaveConfig = add(textButton("Save Configuration (K)")).width(BUTTON_WIDTH).actor
                row()
                buttonLoadAlgorithm = add(textButton("Load Algorithm (X)")).width(BUTTON_WIDTH).actor
                row()
                buttonPutTiles = add(textButton("Put Tiles (T)")).width(BUTTON_WIDTH).actor
                row()
                buttonPutRobots = add(textButton("Put Robots (R)")).width(BUTTON_WIDTH).actor
                row()
                buttonSelectTarget = add(textButton("Select Target Nodes (Z)")).width(BUTTON_WIDTH).actor
            }
        }
    }

    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            logger.error { "Could not set UI look to system look" }
            logger.error { "Exception: $e" }
        }

        buttonLoadConfig.onClick { loadConfiguration() }
        buttonSaveConfig.onClick { saveConfiguration() }
        buttonLoadAlgorithm.onClick { loadAlgorithm() }
        buttonPutTiles.onClick { togglePutTiles() }
        buttonPutRobots.onClick { togglePutRobots() }
        buttonSelectTarget.onClick { toggleSelectTarget() }
    }

    /** Called when a frame is rendered to draw the menu. Menu is only drawn when [active] is true. */
    fun draw() {
        if (active) {
            menuStage.act()
            menuStage.draw()
        }
    }

    /** Called when the window is resized to ensure that the menu remains at the right-hand side of the screen. */
    fun resize(width: Int, height: Int) {
        menuStage.viewport.update(width, height, true)
        menuStage.actors.get(0).setPosition(width / 2f - BUTTON_WIDTH - 6f, 0f)
    }

    /** Opens a file selector window. The user can select a configuration file (JSON format) to be loaded. */
    fun loadConfiguration() {
        if (!active) {
            return
        }
        val configFile = getFile(jsonFilter) ?: return
        if (!configFile.exists()) {
            logger.error { "Cannot load configuration, selected file $configFile does not exist" }
            return
        }
        Configuration.loadConfiguration(configFile.readText())
    }

    /** Opens a file selector window. The user can select a file where the current configuration is to be saved. */
    fun saveConfiguration() {
        if (!active) {
            return
        }
        var configFile = getFile(jsonFilter) ?: return
        if (configFile.extension != "json") {
            configFile = File(configFile.absolutePath.plus(".json"))
        }
        configFile.writeText(Configuration.getJson())
    }

    /** Opens a file selector window. The user can select an algorithm file (kts script) to be loaded. */
    fun loadAlgorithm() {
        if (!active) {
            return
        }
        val algorithmFile = getFile(algoFilter) ?: return
        if (!algorithmFile.exists()) {
            logger.error { "Cannot load algorithm, selected file $algorithmFile does not exist" }
            return
        }
        AlgorithmLoader.loadAlgorithm(algorithmFile)
    }

    /** Toggles whether tiles should be placed by a mouse click. */
    fun togglePutTiles() {
        if (!active) {
            return
        }
        Scheduler.stop()
        val nextBool = !putTiles
        deactivateToggleButtons()
        putTiles = nextBool
        buttonPutTiles.color = if (putTiles) Color.GRAY else Color.WHITE
    }

    /** Toggles whether robots should be placed by a mouse click. */
    fun togglePutRobots() {
        if (!active) {
            return
        }
        Scheduler.stop()
        val nextBool = !putRobots
        deactivateToggleButtons()
        putRobots = nextBool
        buttonPutRobots.color = if (putRobots) Color.GRAY else Color.WHITE
    }

    /** Toggles whether nodes should be marked as target nodes by a mouse click. */
    fun toggleSelectTarget() {
        if (!active) {
            return
        }
        val nextBool = !selectTarget
        Scheduler.stop()
        deactivateToggleButtons()
        selectTarget = nextBool
        buttonSelectTarget.color = if (selectTarget) Color.GRAY else Color.WHITE
    }

    /** Deactivates all toggle buttons. */
    fun deactivateToggleButtons() {
        putTiles = false
        putRobots = false
        selectTarget = false
        buttonPutTiles.color = Color.WHITE
        buttonPutRobots.color = Color.WHITE
        buttonSelectTarget.color = Color.WHITE
    }

    /**
     * Opens a file selector window, [filter] specifies which file endings are allowed.
     * Returns the selected file or null if no file was selected.
     */
    private fun getFile(filter: FileNameExtensionFilter): File? {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = filter
        val choice = fileChooser.showOpenDialog(null)
        if (choice == JFileChooser.APPROVE_OPTION) {
            return fileChooser.selectedFile
        }
        return null
    }

}
