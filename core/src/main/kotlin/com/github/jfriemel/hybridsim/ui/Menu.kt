package com.github.jfriemel.hybridsim.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.Scheduler
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.log.logger
import ktx.scene2d.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private val logger = logger<Menu>()

private const val BUTTON_WIDTH = 190f

private val buttonColorDefault = Color.WHITE
private val buttonColorToggled = Color.GRAY
private val buttonColorDisabled = Color(1f, 1f, 1f, 0.5f)

class Menu(batch: Batch) {

    var screen: SimScreen? = null

    // When active, tiles/robots/target nodes can be added/removed by mouse clicks
    var putTiles = false
    var putRobots = false
    var selectTarget = false

    // Indicates whether the menu is shown on the screen
    private var active = true

    // All menu buttons
    private var buttonLoadConfig: KTextButton
    private var buttonSaveConfig: KTextButton
    private var buttonLoadAlgorithm: KTextButton
    private var buttonPutTiles: KTextButton
    private var buttonPutRobots: KTextButton
    private var buttonSelectTarget: KTextButton
    private var buttonToggleScheduler: KTextButton
    private var buttonUndo: KTextButton
    private var buttonRedo: KTextButton
    private var sliderScheduler: Slider

    // Textures for the scheduler and undo/redo buttons
    private val schedulerOnDrawable =
        TextureRegionDrawable(Texture(Gdx.files.internal("ui/scheduler_on.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        })
    private val schedulerOffDrawable =
        TextureRegionDrawable(Texture(Gdx.files.internal("ui/scheduler_off.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        })
    private var undoTexture = Texture(Gdx.files.internal("ui/undo.png"), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }
    private var undoDrawable = TextureRegionDrawable(undoTexture)
    private var redoDrawable = TextureRegionDrawable(undoTexture).apply { region.flip(true, false) }
    private var schedulerButtonImage: Image
    private var undoButtonImage: Image
    private var redoButtonImage: Image

    // File extension filters for the files used by the simulator
    private val jsonFilter = FileNameExtensionFilter("HybridSim configuration files (.json)", "json")
    private val algoFilter = FileNameExtensionFilter("HybridSim algorithm scripts (.kts)", "kts")

    val menuStage = Stage(ScreenViewport(OrthographicCamera()), batch).apply {
        actors {
            table {
                setFillParent(true)
                defaults().pad(2f).colspan(3).width(BUTTON_WIDTH)
                label("Menu (M)") {
                    color = Color.BLACK
                    setAlignment(Align.center)
                }
                row()
                buttonLoadConfig = textButton("Load Configuration (L)")
                row()
                buttonSaveConfig = textButton("Save Configuration (S)")
                row()
                buttonLoadAlgorithm = textButton("Load Algorithm (A)")
                row()
                buttonPutTiles = textButton("Put Tiles (T)")
                row()
                buttonPutRobots = textButton("Put Robots (R)")
                row()
                buttonSelectTarget = textButton("Select Target Nodes (Z)")
                row()
                buttonUndo = textButton("") {
                    undoButtonImage = image(undoDrawable)
                    cell(colspan = 1, width = undoButtonImage.width)
                }
                buttonToggleScheduler = textButton("") {
                    schedulerButtonImage = image(schedulerOnDrawable)
                    cell(colspan = 1, width = schedulerButtonImage.width)
                }
                buttonRedo = textButton("") {
                    redoButtonImage = image(redoDrawable)
                    cell(colspan = 1, width = redoButtonImage.width)
                }
                row()
                sliderScheduler = slider(0f, 100f, 0.5f)
            }
        }
    }

    // An array of all clickable UI elements to easily enable / disable all at once
    private val inputElements: Array<Actor> = arrayOf(
        buttonLoadConfig,
        buttonSaveConfig,
        buttonLoadAlgorithm,
        buttonPutTiles,
        buttonPutRobots,
        buttonSelectTarget,
        buttonToggleScheduler,
        buttonUndo,
        buttonRedo,
        sliderScheduler,
    )
    private val fileChooserButtons = arrayOf(buttonLoadConfig, buttonSaveConfig, buttonLoadAlgorithm)
    private val toggleButtons = arrayOf(buttonPutTiles, buttonPutRobots, buttonSelectTarget)

    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            logger.error { "Could not set UI look to system look" }
            logger.error { e.toString() }
            logger.error { e.stackTraceToString() }
        }
        buttonLoadConfig.onClick { if (active) loadConfiguration() }
        buttonSaveConfig.onClick { if (active) saveConfiguration(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) }
        buttonLoadAlgorithm.onClick { if (active) loadAlgorithm() }

        buttonPutTiles.onClick { if (active) togglePutTiles() }
        buttonPutRobots.onClick { if (active) togglePutRobots() }
        buttonSelectTarget.onClick { if (active) toggleSelectTarget() }

        buttonUndo.onClick {
            if (active) {
                Scheduler.stop()
                Configuration.undo()
            }
        }
        buttonRedo.onClick {
            if (active) {
                Scheduler.stop()
                Configuration.redo()
            }
        }

        buttonToggleScheduler.onClick {
            if (!active) return@onClick
            untoggleToggleButtons()
            Scheduler.toggle()
        }
        sliderScheduler.onChange {
            if (!active) {
                sliderScheduler.value = max(0f, 100f - sqrt(Scheduler.getIntervalTime().toFloat()))
                return@onChange
            }
            Scheduler.setIntervalTime((100f - sliderScheduler.value).pow(2).toLong())
        }

        sliderScheduler.value = max(0f, 100f - sqrt(Scheduler.getIntervalTime().toFloat()))
    }

    /** @return True if the menu is active (i.e., visible). */
    fun isActive(): Boolean = active

    /** Toggles the visibility of the menu. */
    fun toggleActive() {
        active = !active
        if (active) {
            inputElements.forEach { it.touchable = Touchable.enabled }
            fileChooserButtons.forEach { it.color = buttonColorDefault }
        } else {
            inputElements.forEach { it.touchable = Touchable.disabled }
        }
    }

    /** Called when a frame is rendered to draw the menu. Menu is only drawn when [active] is true. */
    fun draw() {
        if (active) {
            // Enable / disable file chooser buttons
            if (Gdx.graphics.isFullscreen && fileChooserButtons.any { it.isTouchable }) {
                fileChooserButtons.forEach { button ->
                    button.color = buttonColorDisabled
                    button.touchable = Touchable.disabled
                }
            } else if (!Gdx.graphics.isFullscreen && fileChooserButtons.any { !it.isTouchable }) {
                fileChooserButtons.forEach { button ->
                    button.color = buttonColorDefault
                    button.touchable = Touchable.enabled
                }
            }

            // Choose correct texture for scheduler button
            schedulerButtonImage.drawable = if (Scheduler.isRunning()) schedulerOffDrawable else schedulerOnDrawable

            // Enable / disable undo / redo buttons
            if (Configuration.undoSteps() <= 0 && buttonUndo.isTouchable) {
                buttonUndo.color = buttonColorDisabled
                buttonUndo.touchable = Touchable.disabled
            } else if (Configuration.undoSteps() > 0 && !buttonUndo.isTouchable) {
                buttonUndo.color = buttonColorDefault
                buttonUndo.touchable = Touchable.enabled
            }
            if (Configuration.redoSteps() <= 0 && buttonRedo.isTouchable) {
                buttonRedo.color = buttonColorDisabled
                buttonRedo.touchable = Touchable.disabled
            } else if (Configuration.redoSteps() > 0 && !buttonRedo.isTouchable) {
                buttonRedo.color = buttonColorDefault
                buttonRedo.touchable = Touchable.enabled
            }

            // Draw the menu
            menuStage.act()
            menuStage.draw()
        }
    }

    /** Called when the window is resized to ensure that the menu remains at the right-hand side of the screen. */
    fun resize(width: Int, height: Int) {
        menuStage.viewport.update(width, height, true)
        menuStage.actors.get(0).setPosition((width - BUTTON_WIDTH) / 2f - 6f, 0f)
    }

    /** Opens a file selector window. The user can select a [Configuration] file (JSON format) to be loaded. */
    fun loadConfiguration() {
        if (Gdx.graphics.isFullscreen) return

        val configFile = getFile(jsonFilter) ?: return
        if (!configFile.exists()) {
            logger.error { "Cannot load configuration, selected file $configFile does not exist" }
            return
        }
        Configuration.loadConfiguration(configFile.readText())
        screen?.resetCamera()
    }

    /** Opens a file selector window. The user can select a file where the current [Configuration] is to be saved. */
    fun saveConfiguration(prettyPrint: Boolean) {
        if (Gdx.graphics.isFullscreen) return

        var configFile = getFile(jsonFilter, true) ?: return
        if (configFile.extension != "json") {
            configFile = File(configFile.absolutePath.plus(".json"))
        }
        configFile.writeText(Configuration.getJson(prettyPrint))
    }

    /** Opens a file selector window. The user can select an algorithm file (kts script) to be loaded. */
    fun loadAlgorithm() {
        if (Gdx.graphics.isFullscreen) return

        val algorithmFile = getFile(algoFilter) ?: return
        if (!algorithmFile.exists()) {
            logger.error { "Cannot load algorithm, selected file $algorithmFile does not exist" }
            return
        }
        AlgorithmLoader.loadAlgorithm(scriptFile = algorithmFile)
    }

    /** Toggles whether tiles should be placed by a mouse click. */
    fun togglePutTiles() {
        Scheduler.stop()
        putRobots = false
        buttonPutRobots.color = buttonColorDefault
        putTiles = !putTiles
        buttonPutTiles.color = if (putTiles) buttonColorToggled else buttonColorDefault
    }

    /** Toggles whether robots should be placed by a mouse click. */
    fun togglePutRobots() {
        Scheduler.stop()
        val nextBool = !putRobots
        untoggleToggleButtons()
        putRobots = nextBool
        buttonPutRobots.color = if (putRobots) buttonColorToggled else buttonColorDefault
    }

    /** Toggles whether nodes should be marked as target nodes by a mouse click. */
    fun toggleSelectTarget() {
        Scheduler.stop()
        putRobots = false
        buttonPutRobots.color = buttonColorDefault
        selectTarget = !selectTarget
        buttonSelectTarget.color = if (selectTarget) buttonColorToggled else buttonColorDefault
    }

    /** Untoggles all toggle buttons. */
    fun untoggleToggleButtons() {
        putTiles = false
        putRobots = false
        selectTarget = false
        toggleButtons.forEach { it.color = buttonColorDefault }
    }

    /**
     * Opens a file selector window, [filter] specifies which file endings are allowed.
     * Returns the selected [File] or null if no file was selected.
     */
    private fun getFile(filter: FileNameExtensionFilter, save: Boolean = false): File? {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = filter
        fileChooser.currentDirectory = File(System.getProperty("user.dir"))

        // Make sure the file chooser is always visible by creating a proxy frame
        val f = JFrame()
        f.isVisible = true
        f.toFront()
        f.isAlwaysOnTop = true
        f.isVisible = false

        // Open either "Open" or "Save" dialog
        val choice = if (save) {
            fileChooser.showSaveDialog(f)
        } else {
            fileChooser.showOpenDialog(f)
        }
        f.dispose()

        return if (choice == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    }

}
