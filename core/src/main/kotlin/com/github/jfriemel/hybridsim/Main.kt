package com.github.jfriemel.hybridsim

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.github.jfriemel.hybridsim.system.Configuration
import com.github.jfriemel.hybridsim.system.Scheduler
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

private val logger = logger<Main>()

class Main : KtxGame<KtxScreen>() {
    override fun create() {
        // Enable logging
        Gdx.app.logLevel = LOG_DEBUG
        logger.debug { "HybridSim started" }

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

        // Generate random input configuration
        Configuration.generate(50, 1)
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
