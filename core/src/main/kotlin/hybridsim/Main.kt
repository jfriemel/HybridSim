package hybridsim

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.log.Logger
import ktx.log.logger

private val logger : Logger = logger<Main>()

class Main : KtxGame<KtxScreen>() {
    override fun create() {
        val batch : Batch = SpriteBatch()
        Gdx.app.logLevel = LOG_DEBUG
        logger.debug { "HybridSim started" }
        addScreen(SimScreen(batch))
        setScreen<SimScreen>()
    }
}
