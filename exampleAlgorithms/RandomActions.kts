import com.badlogic.gdx.graphics.Color
import com.github.jfriemel.hybridsim.entities.*
import kotlin.random.Random


fun getRobot(orientation: Int, node: Node, carriesTile: Boolean, numPebbles: Int, maxPebbles: Int): Robot {
    return RobotImpl(orientation, node, carriesTile, numPebbles, maxPebbles)
}


class RobotImpl(
    orientation: Int,
    node: Node,
    carriesTile: Boolean,
    numPebbles: Int,
    maxPebbles: Int
): Robot(
    orientation = orientation,
    node = node,
    carriesTile = carriesTile,
    numPebbles = numPebbles,
    maxPebbles = maxPebbles
) {
    private var color: Color = Color.WHITE

    override fun getColor(): Color {
        return color
    }

    override fun activate() {
        color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
        val label = Random.nextInt(0, 6)
        if (isOnTile() && !carriesTile && !tileHasPebble() && Random.nextBoolean()) {
            liftTile()
        } else if (!isOnTile() && carriesTile && Random.nextBoolean()) {
            placeTile()
        } else if (isOnTile() && !tileHasPebble() && numPebbles > 0 && Random.nextBoolean()) {
            putPebble()
        } else if (isOnTile() && tileHasPebble() && numPebbles < maxPebbles && Random.nextBoolean()) {
            takePebble()
        } else if ((isOnTile() || hasTileAtLabel(label)) && !hasRobotAtLabel(label)) {
            moveToLabel(label)
        }
    }
}
