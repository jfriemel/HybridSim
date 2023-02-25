package hybridsim

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import ktx.graphics.use

class SimScreen(private val batch: Batch) : KtxScreen {
    private val viewport = ScreenViewport()
    private val bkgTexture = Texture(Gdx.files.internal("graphics/grid_small.png")).apply {
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
    }
    private val bkgSprite = Sprite(bkgTexture)

    override fun render(delta: Float) {
        viewport.apply()
        batch.use(viewport.camera.combined) {
            bkgSprite.draw(it)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        bkgSprite.setRegion(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        bkgSprite.setSize(width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        bkgTexture.dispose()
    }
}
