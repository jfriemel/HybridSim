package hybridsim

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.ceil

class SimScreen(private val batch: Batch) : KtxScreen, InputProcessor {
    private val camera = OrthographicCamera();
    private val viewport = ScreenViewport(camera)
    private val bkgTexture = Texture(Gdx.files.internal("graphics/grid.png")).apply {
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
    }
    private val bkgSprite = Sprite(bkgTexture)

    init {
        Gdx.input.inputProcessor = this
    }

    override fun render(delta: Float) {
        viewport.apply()
        camera.update()
        batch.use(viewport.camera.combined) {
            bkgSprite.draw(it)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.unitsPerPixel += 0.05f
        val factor = viewport.unitsPerPixel
        viewport.update(width, height, true)
        bkgSprite.setRegion(0, 0, ceil(factor * Gdx.graphics.width).toInt(), ceil(factor * Gdx.graphics.height).toInt())
        bkgSprite.setSize(factor * width, factor * height)
    }

    override fun dispose() {
        bkgTexture.dispose()
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val nextZoom = viewport.unitsPerPixel + amountY
        if (nextZoom > 0) {
            viewport.unitsPerPixel = nextZoom
            val factor = viewport.unitsPerPixel
            bkgSprite.setRegion(0, 0, ceil(factor * Gdx.graphics.width).toInt(), ceil(factor * Gdx.graphics.height).toInt())
            return true
        }
        return false
    }
}
