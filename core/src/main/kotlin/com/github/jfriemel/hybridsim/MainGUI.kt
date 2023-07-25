package com.github.jfriemel.hybridsim

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.GeneratorLoader
import com.github.jfriemel.hybridsim.system.Scheduler
import com.github.jfriemel.hybridsim.ui.AlgorithmLoader
import com.github.jfriemel.hybridsim.ui.InputHandler
import com.github.jfriemel.hybridsim.ui.Menu
import com.github.jfriemel.hybridsim.ui.SimScreen
import com.github.tommyettinger.textra.KnownFonts
import com.kotcrab.vis.ui.VisUI
import kotlinx.coroutines.launch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.log.logger
import ktx.scene2d.Scene2DSkin
import java.io.File

private val logger = logger<MainGUI>()

class MainGUI(
    private val algorithm: File? = null,
    private val configuration: File? = null,
    private val generator: File? = null,
) : KtxGame<KtxScreen>() {
    override fun create() {
        // Enable logging
        Gdx.app.logLevel = Application.LOG_ERROR
        logger.debug { "HybridSim (GUI) launched" }

        // Set UI look
        VisUI.load()
        Scene2DSkin.defaultSkin = VisUI.getSkin()
        KnownFonts.setAssetPrefix("ui/fonts/")

        // Launch main program
        val batch = SpriteBatch()
        val menu = Menu(batch)
        val screen = SimScreen(batch, menu)
        addScreen(screen)
        setScreen<SimScreen>()
        val inputMultiplexer = InputMultiplexer()
        inputMultiplexer.addProcessor(menu.menuStage)
        inputMultiplexer.addProcessor(InputHandler(screen, menu))
        Gdx.input.inputProcessor = inputMultiplexer

        // Load algorithm
        algorithm?.let { file -> AlgorithmLoader.loadAlgorithm(file) }

        // Load generator
        generator?.let { file -> GeneratorLoader.loadGenerator(file) }

        // Load configuration or generate random configuration
        configuration?.let { file -> Configuration.loadConfiguration(file.readText()) }
            ?: Configuration.generate(50, 1)
        Configuration.clearUndoQueues()
        screen.resetCamera()

        // Start a new coroutine for the scheduler
        KtxAsync.initiate()
        launchScheduler()
    }

    private fun launchScheduler() {
        KtxAsync.launch {
            Scheduler.run()
        }
    }
}
