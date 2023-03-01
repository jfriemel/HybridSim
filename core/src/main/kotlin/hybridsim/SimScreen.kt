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
import kotlin.math.min
import kotlin.math.sqrt

class SimScreen(private val batch: Batch) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ScreenViewport(camera).apply {
        unitsPerPixel = 16f  // Reasonable initial zoom level
    }
    private val bkgTexture = Texture(Gdx.files.internal("graphics/grid.png")).apply {
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)  // Grid should be infinite
    }
    private val bkgSprite = Sprite(bkgTexture)

    // Number of pixels corresponding to one horizontal unit in the triangular lattice
    private val pixelUnitDistance = bkgTexture.width / 2

    // The initial configuration is centred around (0,0)
    private var xPos = - viewport.unitsPerPixel * Gdx.graphics.width / 2f
    private var yPos = - viewport.unitsPerPixel * Gdx.graphics.height / 2f

    // How much the map moves per frame, only relevant while arrow keys (or WASD) are pressed
    var xMomentum = 0
    var yMomentum = 0

    override fun render(delta: Float) {
        move(xMomentum, yMomentum)  // Movement speed depends on FPS, is that an issue?
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

    /**
     * Zoom in (amount < 0) or out (amount > 0) towards the centre or the mouse if mouse coordinates are given.
     *
     * @param amount Zoom factor. Negative: Zoom in. Positive: Zoom out.
     * @param mouseX X position of the mouse. Default: Horizontal centre of the screen.
     * @param mouseY Y position of the mouse. Default: Vertical centre of the screen.
     */
    fun zoom(amount: Float, mouseX: Int = Gdx.graphics.width / 2, mouseY: Int = Gdx.graphics.height / 2) {
        val previousUPP = viewport.unitsPerPixel
        viewport.unitsPerPixel = min(max(previousUPP + amount, 1f), 80f)
        val zoomFactor = previousUPP/viewport.unitsPerPixel - 1f
        move((mouseX * zoomFactor).toInt(), (mouseY * zoomFactor).toInt())
    }

    /**
     * Move the scene in specified direction.
     *
     * @param xDir X direction.
     * @param yDir Y direction.
     */
    fun move(xDir: Int, yDir: Int) {
        val factor = viewport.unitsPerPixel
        xPos += factor * xDir
        yPos += factor * yDir
        moveBackground()
    }

    fun screenCoordsToNodeCoords(screenX: Int, screenY: Int): Pair<Float, Float> {
        //TODO("Find out how to do this")
        var x = ((viewport.unitsPerPixel * screenY + yPos) / (pixelUnitDistance * sqrt(3f) / 2) + (viewport.unitsPerPixel * screenX + xPos) / pixelUnitDistance) / 2
        val y = (viewport.unitsPerPixel * screenY + yPos) / pixelUnitDistance
        x = (viewport.unitsPerPixel * screenX + xPos) / pixelUnitDistance - y / sqrt(3f)
        println("$x  $y")
        return Pair(x, y)
    }

    /**
     * Moves the background (i.e. the background texture) to correspond to the current x and y position of the scene.
     */
    private fun moveBackground() {
        val factor = viewport.unitsPerPixel
        val width = ceil(factor * Gdx.graphics.width)
        val height = ceil(factor * Gdx.graphics.height)
        bkgSprite.setRegion(xPos.toInt(), yPos.toInt(), width.toInt(), height.toInt())
    }
}
