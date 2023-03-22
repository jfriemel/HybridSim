import kotlin.random.Random

fun getRobot(orientation: Int, node: Node): Robot {
    return RobotImpl(orientation, node)
}

class RobotImpl(orientation: Int, node: Node): Robot(
    orientation = orientation,
    node = node,
    carriesTile = false,
    numPebbles = 2,
    maxPebbles = 2
) {
    private var color = Color.WHITE

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

    override fun getColor() = color
}
