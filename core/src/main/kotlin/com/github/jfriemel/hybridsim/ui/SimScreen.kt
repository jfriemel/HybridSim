package com.github.jfriemel.hybridsim.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.jfriemel.hybridsim.entities.Entity
import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import com.github.jfriemel.hybridsim.system.Configuration
import ktx.app.KtxScreen
import ktx.graphics.use
import ktx.log.logger
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

private val logger = logger<SimScreen>()

private const val INITIAL_ZOOM = 16f // Reasonable initial zoom level

class SimScreen(
    private val batch: Batch,
    private val menu: Menu,
) : KtxScreen {
    // Squeeze factor to make the triangles equilateral (the texture is stretched horizontally)
    private val xScale = sqrt(3f) / 2f

    private val camera = OrthographicCamera()
    private val viewport = ScreenViewport(camera).apply { unitsPerPixel = INITIAL_ZOOM }

    // Background texture (triangular lattice)
    private val bkgTexture =
        Texture(Gdx.files.internal("graphics/grid.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
            setWrap(
                Texture.TextureWrap.Repeat,
                Texture.TextureWrap.Repeat,
            ) // Grid should be infinite
        }
    private val bkgSprite = Sprite(bkgTexture)

    // Robot textures
    private val robotTexture =
        Texture(Gdx.files.internal("graphics/robot.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }

    // Tile textures
    private val tileTexture =
        Texture(Gdx.files.internal("graphics/tile.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    private val tilePebbleTexture =
        Texture(Gdx.files.internal("graphics/tile_pebble.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }

    private val emptyTargetTexture =
        Texture(Gdx.files.internal("graphics/empty_target.png"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    private val emptyTargetSprites = HashMap<Node, Sprite>()

    // Number of pixels corresponding to one horizontal unit in the triangular lattice
    private val pixelUnitDistance = bkgTexture.height

    // Keep track of the x and y offset of the triangular lattice texture
    private var xPos = 0f
    private var yPos = 0f

    // Keep track of window width and height to maintain the same center point when resizing the
    // window
    private var width = 0
    private var height = 0

    // Keep track of monitor, update frame rate if monitor changes
    private var monitor = Gdx.graphics.monitor

    // How much the map moves per frame, only relevant when arrow keys are pressed
    var xMomentum = 0
    var yMomentum = 0

    init {
        menu.screen = this
    }

    override fun render(delta: Float) {
        // Move the map when arrow keys are pressed
        move(xMomentum, yMomentum)

        batch.use(viewport.camera.combined) { batch ->
            // Draw triangular lattice
            bkgSprite.draw(batch)

            // Draw tiles
            Configuration.tiles.values.forEach { tile -> tile.sprite?.draw(batch) }

            // Draw empty target nodes
            Configuration.targetNodes
                .filterNot { node -> node in Configuration.tiles }
                .forEach { node -> emptyTargetSprites[node]?.draw(batch) }

            // Draw robots
            Configuration.robots.values.forEach { robot ->
                if (robot.carriesTile) {
                    robot.carrySprite?.draw(batch)
                }
                robot.sprite?.draw(batch)
            }
        }

        // Draw menu
        menu.draw()
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        logger.debug { "Resized window: width = $width, height = $height" }

        // Update frame rate when switching monitors
        if (Gdx.graphics.monitor != monitor) {
            monitor = Gdx.graphics.monitor
            Gdx.graphics.setForegroundFPS(Gdx.graphics.displayMode.refreshRate)
        }

        // Update viewport and move to maintain center node
        viewport.update(width, height, true)
        move((this.width - width) / 2, (this.height - height) / 2)
        this.width = width
        this.height = height

        // Update texture cutout to fit new window size
        bkgSprite.setSize(
            ceil(viewport.unitsPerPixel * width),
            ceil(viewport.unitsPerPixel * height),
        )
        moveBackground()

        // Keep menu on the right hand side of the screen
        menu.resize(width, height)
    }

    override fun dispose() =
        arrayOf(
            bkgTexture,
            robotTexture,
            tileTexture,
            tilePebbleTexture,
            emptyTargetTexture,
        ).forEach(Texture::dispose)

    /**
     * Zoom towards ([amount] < 0) or away from ([amount] > 0) the screen center or the mouse if
     * mouse coordinates ([mouseX], [mouseY]) are given.
     */
    fun zoom(
        amount: Float,
        mouseX: Int = width / 2,
        mouseY: Int = height / 2,
    ) {
        val previousUPP = viewport.unitsPerPixel
        viewport.unitsPerPixel = min(max(previousUPP + amount, 1f), 80f)

        viewport.update(width, height, true)
        bkgSprite.setSize(
            ceil(viewport.unitsPerPixel * width),
            ceil(viewport.unitsPerPixel * height),
        )

        val zoomFactor = previousUPP / viewport.unitsPerPixel - 1f
        move((mouseX * zoomFactor).toInt(), (mouseY * zoomFactor).toInt())
    }

    /** Move the scene in specified direction ([xDir], [yDir]). */
    fun move(
        xDir: Int,
        yDir: Int,
    ) {
        xPos += viewport.unitsPerPixel * xDir / xScale
        yPos += viewport.unitsPerPixel * yDir
        setEntityScreenPositions(Configuration.tiles)
        setEntityScreenPositions(Configuration.robots)
        setTargetScreenPositions()
        moveBackground()
    }

    /** Convert screen / pixel coordinates ([screenX], [screenY]) to a [Node]. */
    fun screenCoordsToNodeCoords(
        screenX: Int,
        screenY: Int,
    ): Node {
        // Not exact, but good enough
        val x =
            round(((viewport.unitsPerPixel / xScale) * screenX + xPos) / pixelUnitDistance).toInt()
        val offset = if (x.mod(2) == 0) 0f else 0.5f // Every second column is slightly offset
        val y =
            round((viewport.unitsPerPixel * screenY + yPos) / pixelUnitDistance + offset).toInt()
        return Node(x, y)
    }

    /**
     * Reset the zoom level and point the camera to the center of the [Tile] configuration or the
     * origin.
     */
    fun resetCamera() {
        // Reset zoom level
        zoom(INITIAL_ZOOM - viewport.unitsPerPixel)

        // Reset camera position
        xPos = 0f
        yPos = 0f

        // Point camera to center of tile configuration or origin if no tiles exist
        val occupiedNodes =
            Configuration.tiles.keys
                .union(Configuration.targetNodes)
                .union(Configuration.robots.keys)
        val centerNode =
            if (occupiedNodes.isEmpty()) {
                Node.origin
            } else {
                val centerX = (occupiedNodes.minOf(Node::x) + occupiedNodes.maxOf(Node::x)) / 2
                val centerY = (occupiedNodes.minOf(Node::y) + occupiedNodes.maxOf(Node::y)) / 2
                Node(centerX, centerY)
            }
        val coords = nodeCoordsToScreenCoords(centerNode.x, centerNode.y)
        move(-width / 2 + coords.first, -height / 2 + coords.second)
    }

    /**
     * Convert [Node] coordinates ([nodeX], [nodeY]) to a [Pair] of screen / pixel coordinates (x,
     * y).
     */
    private fun nodeCoordsToScreenCoords(
        nodeX: Int,
        nodeY: Int,
    ): Pair<Int, Int> {
        val offset = if (nodeX.mod(2) == 0) 0f else 0.5f // Every second column is slightly offset
        val x = round((nodeX * pixelUnitDistance - xPos) * xScale / viewport.unitsPerPixel)
        val y = round(((nodeY - offset) * pixelUnitDistance - yPos) / viewport.unitsPerPixel)
        return Pair(x.toInt(), y.toInt())
    }

    /**
     * Move the background texture to correspond to the current x and y position ([xPos], [yPos]) of
     * the scene.
     */
    private fun moveBackground() {
        val width = ceil(viewport.unitsPerPixel * width / xScale)
        val height = ceil(viewport.unitsPerPixel * height)
        bkgSprite.setRegion(xPos.toInt(), yPos.toInt(), width.toInt(), height.toInt())
    }

    /**
     * Assign sprites with the correct textures to all [entities] ([Robot]s, [Tile]s) that do not
     * have sprites yet. Then set the sprites' screen position variables such that the [entities]
     * are drawn at the correct locations.
     */
    private fun setEntityScreenPositions(entities: Map<Node, Entity>) {
        for (entity in entities.values) {
            val texture =
                when (entity) {
                    is Robot -> robotTexture
                    is Tile -> if (entity.hasPebble()) tilePebbleTexture else tileTexture
                    else -> Texture("")
                }

            if (entity.sprite == null) {
                entity.sprite = Sprite(texture)
            } else { // Always update texture in case it changes (e.g., tile has a pebble now)
                entity.sprite?.texture = texture
                entity.sprite?.color = entity.getColor()
            }

            val coords = nodeCoordsToScreenCoords(entity.node.x, entity.node.y)
            val x = viewport.unitsPerPixel * coords.first
            val y = viewport.unitsPerPixel * (height - coords.second)

            entity.sprite?.setPosition(x - texture.width / 2f, y - texture.height / 2f)

            if (entity is Robot && entity.carriesTile) {
                if (entity.carrySprite == null) {
                    entity.carrySprite = Sprite(tileTexture).apply { setScale(0.55f) }
                }
                entity.carrySprite?.setPosition(
                    x - tileTexture.width / 2f,
                    y - tileTexture.height / 2f,
                )
            }
        }
    }

    /**
     * Create sprites for target nodes that do not have sprites yet. Then set the sprites' screen
     * position variables such that they are drawn at their nodes' positions.
     */
    private fun setTargetScreenPositions() {
        for (targetNode in Configuration.targetNodes) {
            if (targetNode !in emptyTargetSprites) {
                emptyTargetSprites[targetNode] =
                    Sprite(emptyTargetTexture).apply { color = Tile.colorTarget }
            }
            val coords = nodeCoordsToScreenCoords(targetNode.x, targetNode.y)
            val x = viewport.unitsPerPixel * coords.first
            val y = viewport.unitsPerPixel * (height - coords.second)
            emptyTargetSprites[targetNode]?.setPosition(
                x - emptyTargetTexture.width / 2,
                y - emptyTargetTexture.height / 2,
            )
        }
    }
}
