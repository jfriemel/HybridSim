package hybridsim

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.viewport.ScreenViewport
import hybridsim.entities.Entity
import hybridsim.entities.Node
import hybridsim.entities.Robot
import hybridsim.entities.Tile
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.*

// Squeeze factor to make the triangles equilateral
val X_SCALE = sqrt(3f) / 2f

class SimScreen(private val batch: Batch) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ScreenViewport(camera).apply {
        unitsPerPixel = 16f  // Reasonable initial zoom level
    }

    // Background texture (triangular lattice)
    private val bkgTexture = Texture(Gdx.files.internal("graphics/grid.png"), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)  // Grid should be infinite
    }
    private val bkgSprite = Sprite(bkgTexture)

    // Robot textures
    private val robotTexture = Texture(Gdx.files.internal("graphics/robot.png"), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }

    // Tile textures
    private val tileTexture = Texture(Gdx.files.internal("graphics/tile.png"), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }
    private val tilePebbleTexture = Texture(Gdx.files.internal("graphics/tile_pebble.png"), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }

    // Number of pixels corresponding to one horizontal unit in the triangular lattice
    private val pixelUnitDistance = bkgTexture.height

    // The initial configuration is centred around (0,0)
    private var xPos = - viewport.unitsPerPixel * Gdx.graphics.width / (2f * X_SCALE)
    private var yPos = - viewport.unitsPerPixel * Gdx.graphics.height / 2f

    // How much the map moves per frame, only relevant while arrow keys (or WASD) are pressed
    var xMomentum = 0
    var yMomentum = 0

    override fun render(delta: Float) {
        move(xMomentum, yMomentum)  // Movement speed depends on FPS, is that an issue?
        batch.use(viewport.camera.combined) {
            bkgSprite.draw(it)
            for (tile in Configuration.tiles.values) {
                tile.sprite?.draw(it)
            }
            for (robot in Configuration.robots.values) {
                robot.sprite?.draw(it)
            }
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

        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        bkgSprite.setSize(ceil(viewport.unitsPerPixel * Gdx.graphics.width), ceil(viewport.unitsPerPixel * Gdx.graphics.height))

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
        xPos += viewport.unitsPerPixel * xDir / X_SCALE
        yPos += viewport.unitsPerPixel * yDir
        setEntityScreenPositions(Configuration.tiles)
        setEntityScreenPositions(Configuration.robots)
        moveBackground()
    }

    fun screenCoordsToNodeCoords(screenX: Int, screenY: Int): Node {
        val x = round(((viewport.unitsPerPixel / X_SCALE) * screenX + xPos) / pixelUnitDistance).toInt()
        val offset = if (x.mod(2) == 0) 0f else 0.5f  // Every second column is slightly offset
        val y = round((viewport.unitsPerPixel * screenY + yPos) / pixelUnitDistance + offset).toInt()
        return Node(x, y)
    }

    fun nodeCoordsToScreenCoords(nodeX: Int, nodeY: Int): Pair<Int, Int> {
        val offset = if (nodeX.mod(2) == 0) 0f else 0.5f  // Every second column is slightly offset
        val x = round((nodeX * pixelUnitDistance - xPos) * X_SCALE / viewport.unitsPerPixel)
        val y = round(((nodeY - offset) * pixelUnitDistance - yPos) / viewport.unitsPerPixel)
        return Pair(x.toInt(), y.toInt())
    }

    /**
     * Moves the background (i.e. the background texture) to correspond to the current x and y position of the scene.
     */
    private fun moveBackground() {
        val width = ceil(viewport.unitsPerPixel * Gdx.graphics.width / X_SCALE)
        val height = ceil(viewport.unitsPerPixel * Gdx.graphics.height)
        bkgSprite.setRegion(xPos.toInt(), yPos.toInt(), width.toInt(), height.toInt())
    }
    private fun setEntityScreenPositions(entities: Map<Node, Entity>) {
        for (entity in entities.values) {
            val texture = when (entity) {
                is Robot -> robotTexture
                is Tile -> if (entity.hasPebble()) tilePebbleTexture else tileTexture
                else -> Texture("")
            }

            if (entity.sprite == null) {
                entity.sprite = Sprite(texture)
            } else {  // Always update texture in case it changes (e.g. tile has a pebble now)
                entity.sprite?.texture = texture
                entity.sprite?.color = entity.color
            }

            val coords = nodeCoordsToScreenCoords(entity.node.x, entity.node.y)
            val x = viewport.unitsPerPixel * coords.first - texture.width / 2f
            val y = viewport.unitsPerPixel * (Gdx.graphics.height - coords.second) - texture.height / 2f
            entity.sprite?.setPosition(x, y)
        }
    }
}
