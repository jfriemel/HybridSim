package hybridsim

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import hybridsim.ui.Menu
import hybridsim.ui.SimScreen
import kotlinx.coroutines.launch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.log.Logger
import ktx.log.logger
import ktx.scene2d.Scene2DSkin

private val logger : Logger = logger<Main>()

class Main : KtxGame<KtxScreen>() {
    override fun create() {
        val batch : Batch = SpriteBatch()
        Gdx.app.logLevel = LOG_DEBUG
        logger.debug { "HybridSim started" }
        Scene2DSkin.defaultSkin = Skin(Gdx.files.internal("ui/uiskin.json"))
        val screen = SimScreen(batch, Menu(batch))
        addScreen(screen)
        setScreen<SimScreen>()
        Gdx.input.inputProcessor = InputHandler(screen)

        // Start a new coroutine for the scheduler
        KtxAsync.initiate()
        launchScheduler()
        Scheduler.start()
    }

    private fun launchScheduler() {
        KtxAsync.launch {
            Scheduler.run()
        }
    }
}
