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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.jfriemel.hybridsim.system.AlgorithmLoader
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.GeneratorLoader
import com.github.jfriemel.hybridsim.system.Scheduler
import com.github.tommyettinger.textra.KnownFonts
import com.github.tommyettinger.textra.TextraLabel
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisSlider
import com.kotcrab.vis.ui.widget.VisTextField
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.runBlocking
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.onKeyUp
import ktx.log.logger
import ktx.scene2d.actors
import ktx.scene2d.vis.*
import java.io.File
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow

private val logger = logger<Menu>()

private const val BUTTON_WIDTH = 192f
private const val BUTTON_HEIGHT = 32f
private const val BUTTON_PAD = 2f

private val buttonColorDefault = Color.WHITE
private val buttonColorToggled = Color.ROYAL
private val buttonColorDisabled = Color(1f, 1f, 1f, 0.5f)

class Menu(
    batch: Batch,
) {
    var screen: SimScreen? = null

    // When active, tiles/robots/target nodes can be added/removed by mouse clicks
    var putTiles = false
    var putRobots = false
    var selectTarget = false

    // Indicates whether the menu is shown on the screen
    private var active = true

    // All menu buttons
    private var buttonLoadConfig: KVisTextButton
    private var buttonSaveConfig: KVisTextButton
    private var buttonLoadAlgorithm: KVisTextButton
    private var buttonPutTiles: KVisTextButton
    private var buttonPutRobots: KVisTextButton
    private var buttonSelectTarget: KVisTextButton
    private var buttonToggleScheduler: KVisTextButton
    private var buttonUndo: KVisTextButton
    private var buttonRedo: KVisTextButton
    private var sliderScheduler: VisSlider
    private var buttonLoadGenerator: KVisTextButton
    private var textFieldTiles: VisTextField
    private var textFieldRobots: VisTextField
    private var checkBoxOverhang: VisCheckBox
    private var textFieldOverhang: VisTextField
    private var buttonGenerate: KVisTextButton

    // Textures for the scheduler and undo/redo buttons
    private val schedulerOnDrawable =
        TextureRegionDrawable(
            Texture(Gdx.files.internal("ui/scheduler_on.png"), true).apply {
                setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
            },
        )
    private val schedulerOffDrawable =
        TextureRegionDrawable(
            Texture(Gdx.files.internal("ui/scheduler_off.png"), true).apply {
                setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
            },
        )
    private var undoTexture =
        Texture(Gdx.files.internal("ui/undo.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    private var undoDrawable = TextureRegionDrawable(undoTexture)
    private var redoDrawable = TextureRegionDrawable(undoTexture).apply { region.flip(true, false) }
    private var schedulerButtonImage: VisImage
    private var undoButtonImage: VisImage
    private var redoButtonImage: VisImage

    // File extension filters for the files used by the simulator
    private val jsonFileName = "HybridSim configuration file (.json)"
    private val jsonFileExtension = "json"
    private val algoFileName = "HybridSim algorithm script (.kts)"
    private val algoFileExtension = "kts"
    private val genFileName = "HybridSim configuration generator script (.kts)"
    private val genFileExtension = "kts"

    val menuStage =
        Stage(ScreenViewport(OrthographicCamera()), batch).apply {
            actors {
                visTable {
                    setFillParent(true)
                    defaults()
                        .pad(BUTTON_PAD)
                        .colspan(3)
                        .width(BUTTON_WIDTH)
                        .minHeight(BUTTON_HEIGHT)
                    add(
                        TextraLabel("[*]Menu (M)", KnownFonts.getGoNotoUniversal()).apply {
                            color = Color.BLACK
                            alignment = Align.center
                        },
                    )
                    row()
                    buttonLoadConfig = visTextButton("Load Configuration (L)")
                    row()
                    buttonSaveConfig = visTextButton("Save Configuration (S)")
                    row()
                    buttonLoadAlgorithm = visTextButton("Load Algorithm (A)")
                    row()
                    buttonPutTiles = visTextButton("Put Tiles (T)")
                    row()
                    buttonPutRobots = visTextButton("Put Robots (R)")
                    row()
                    buttonSelectTarget = visTextButton("Select Target Nodes (Z)")
                    row()
                    buttonUndo =
                        visTextButton("") {
                            undoButtonImage = visImage(undoDrawable)
                            cell(colspan = 1, width = undoButtonImage.width, align = Align.center)
                        }
                    buttonToggleScheduler =
                        visTextButton("") {
                            schedulerButtonImage = visImage(schedulerOnDrawable)
                            cell(
                                colspan = 1,
                                width = schedulerButtonImage.width,
                                align = Align.center,
                            )
                        }
                    buttonRedo =
                        visTextButton("") {
                            redoButtonImage = visImage(redoDrawable)
                            cell(colspan = 1, width = redoButtonImage.width, align = Align.center)
                        }
                    row()
                    sliderScheduler = visSlider(0f, 44f, 0.1f)
                    row()
                    buttonLoadGenerator = visTextButton("Load Generator (H)")
                    row()
                    visLabel("# tiles:").apply {
                        cell(colspan = 1, width = undoButtonImage.width, align = Align.left)
                        color = Color.BLACK
                    }
                    textFieldTiles =
                        visTextField("50").apply {
                            cell(colspan = 2, width = BUTTON_WIDTH / 2, align = Align.right)
                        }
                    row()
                    visLabel("# robots:").apply {
                        cell(colspan = 1, width = undoButtonImage.width, align = Align.left)
                        color = Color.BLACK
                    }
                    textFieldRobots =
                        visTextField("1").apply {
                            cell(colspan = 2, width = BUTTON_WIDTH / 2, align = Align.right)
                        }
                    row()
                    visLabel("Generate target shape:").apply {
                        cell(
                            colspan = 2,
                            width = undoButtonImage.width + schedulerButtonImage.width,
                            align = Align.left,
                        )
                        color = Color.BLACK
                    }
                    checkBoxOverhang =
                        visCheckBox("").apply {
                            cell(colspan = 1, width = redoButtonImage.width, align = Align.right)
                        }
                    row()
                    visLabel("# overhangs:").apply {
                        cell(colspan = 1, width = undoButtonImage.width, align = Align.left)
                        color = Color.BLACK
                    }
                    textFieldOverhang =
                        visTextField("15").apply {
                            cell(colspan = 2, width = BUTTON_WIDTH / 2, align = Align.right)
                        }
                    row()
                    buttonGenerate = visTextButton("Generate (G)")
                }
            }
        }

    // An array of all clickable UI elements to easily enable / disable all at once
    private val inputElements: Array<Actor> =
        arrayOf(
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
            buttonLoadGenerator,
            textFieldTiles,
            textFieldRobots,
            checkBoxOverhang,
            textFieldOverhang,
            buttonGenerate,
        )
    private val fileChooserButtons =
        arrayOf(buttonLoadConfig, buttonSaveConfig, buttonLoadAlgorithm, buttonLoadGenerator)
    private val toggleButtons = arrayOf(buttonPutTiles, buttonPutRobots, buttonSelectTarget)

    init {
        buttonLoadConfig.onClick { if (active) loadConfiguration() }
        buttonSaveConfig.onClick {
            if (active) saveConfiguration(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
        }
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
                sliderScheduler.value = max(0f, 45f - cbrt(Scheduler.getIntervalTime().toFloat()))
                return@onChange
            }
            Scheduler.setIntervalTime((45f - sliderScheduler.value).pow(3).toLong())
        }

        sliderScheduler.value = max(0f, 45f - cbrt(Scheduler.getIntervalTime().toFloat()))

        buttonLoadGenerator.onChange { if (active) loadGenerator() }

        textFieldTiles.onKeyUp {
            if (text.any { !it.isDigit() }) {
                clearText()
            }
        }
        textFieldRobots.onKeyUp {
            if (text.any { !it.isDigit() }) {
                clearText()
            }
        }
        textFieldOverhang.onKeyUp {
            if (text.any { !it.isDigit() }) {
                clearText()
            }
        }

        buttonGenerate.onChange { if (active) generateConfiguration() }
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

    /**
     * Called when a frame is rendered to draw the menu. Menu is only drawn when [active] is true.
     */
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
            schedulerButtonImage.drawable =
                if (Scheduler.isRunning()) schedulerOffDrawable else schedulerOnDrawable

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

    /**
     * Called when the window is resized to ensure that the menu remains at the right-hand side of
     * the screen.
     */
    fun resize(
        width: Int,
        height: Int,
    ) {
        menuStage.viewport.update(width, height, true)
        menuStage.actors.get(0).setPosition((width - BUTTON_WIDTH) / 2f - 6f, 0f)
    }

    /**
     * Opens a file selector window. The user can select a [Configuration] file (JSON format) to be
     * loaded.
     */
    fun loadConfiguration() {
        if (Gdx.graphics.isFullscreen) return

        val configFile = getFile(jsonFileName, jsonFileExtension) ?: return
        if (!configFile.exists()) {
            logger.error { "Cannot load configuration, selected file $configFile does not exist" }
            return
        }
        Configuration.loadConfiguration(configFile.readText())
        screen?.resetCamera()
    }

    /**
     * Opens a file selector window. The user can select a file where the current [Configuration] is
     * to be saved.
     */
    fun saveConfiguration(prettyPrint: Boolean) {
        if (Gdx.graphics.isFullscreen) return

        var configFile = getFile(jsonFileName, jsonFileExtension) ?: return
        if (configFile.extension != "json") {
            configFile = File(configFile.absolutePath.plus(".json"))
        }
        configFile.writeText(Configuration.getJson(prettyPrint))
    }

    /**
     * Opens a file selector window. The user can select an algorithm file (kts script) to be
     * loaded.
     */
    fun loadAlgorithm() {
        if (Gdx.graphics.isFullscreen) return

        val algorithmFile = getFile(algoFileName, algoFileExtension) ?: return
        if (!algorithmFile.exists()) {
            logger.error { "Cannot load algorithm, selected file $algorithmFile does not exist" }
            return
        }
        AlgorithmLoader.loadAlgorithm(scriptFile = algorithmFile)
    }

    /**
     * Opens a file selector window. The user can select a configuration generator file (kts script)
     * to be loaded.
     */
    fun loadGenerator() {
        if (Gdx.graphics.isFullscreen) return

        val generatorFile = getFile(genFileName, genFileExtension) ?: return
        if (!generatorFile.exists()) {
            logger.error { "Cannot load generator, selected file $generatorFile does not exist" }
            return
        }
        GeneratorLoader.loadGenerator(scriptFile = generatorFile)
    }

    /** Generates a new configuration. */
    fun generateConfiguration() {
        val numTiles = if (textFieldTiles.isEmpty) 0 else textFieldTiles.text.toInt()
        val numRobots = if (textFieldRobots.isEmpty) 0 else textFieldRobots.text.toInt()
        val numOverhang = if (textFieldOverhang.isEmpty) 0 else textFieldOverhang.text.toInt()

        if (checkBoxOverhang.isChecked) {
            Configuration.generate(numTiles, numRobots, numOverhang)
        } else {
            Configuration.generate(numTiles, numRobots)
        }
        screen?.resetCamera()
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
     * Opens a file selector window. [fileName] and [fileExtension] specify which file types are
     * allowed. Returns the selected [File] or null if no file was selected.
     */
    private fun getFile(
        fileName: String,
        fileExtension: String,
    ): File? =
        runBlocking {
            FileKit
                .pickFile(
                    type = PickerType.File(extensions = listOf(fileExtension)),
                    mode = PickerMode.Single,
                    title = fileName,
                    initialDirectory = System.getProperty("user.dir"),
                )?.file
        }
}
