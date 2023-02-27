package hybridsim

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.ceil
import kotlin.math.max

class SimScreen(private val batch: Batch) : KtxScreen {
    private val camera = OrthographicCamera();
    private val viewport = ScreenViewport(camera).apply {
        unitsPerPixel = 16f  // Reasonable initial zoom level
    }
    private val bkgTexture = Texture(Gdx.files.internal("graphics/grid.png")).apply {
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)  // Grid should be infinite
    }
    private val bkgSprite = Sprite(bkgTexture)

    private var xPos = 0f
    private var yPos = 0f

    var xMomentum = 0
    var yMomentum = 0

    override fun render(delta: Float) {
        move(xMomentum, yMomentum)
        batch.use(viewport.camera.combined) {
            bkgSprite.draw(it)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        bkgSprite.setSize(ceil(viewport.unitsPerPixel * width), ceil(viewport.unitsPerPixel * height))
        moveBackground()
    }

    override fun dispose() {
        bkgTexture.dispose()
    }

    fun zoom(amount: Float, mouseX: Int = 0, mouseY: Int = 0) {
        val previousUPP = viewport.unitsPerPixel
        viewport.unitsPerPixel = max(previousUPP + amount, 1f)
        val zoomFactor = previousUPP/viewport.unitsPerPixel - 1f
        move((mouseX * zoomFactor).toInt(), (mouseY * zoomFactor).toInt())
    }

    private fun move(xDir: Int, yDir: Int) {
        val factor = viewport.unitsPerPixel
        xPos += factor * xDir
        yPos += factor * yDir
        moveBackground()
    }

    private fun moveBackground() {
        val factor = viewport.unitsPerPixel
        val width = ceil(factor * Gdx.graphics.width)
        val height = ceil(factor * Gdx.graphics.height)
        bkgSprite.setRegion(xPos.toInt(), yPos.toInt(), width.toInt(), height.toInt())
    }
}
