/* Single-robot line formation algorithm from https://doi.org/10.1007/s11047-019-09774-2 */

fun getRobot(orientation: Int, node: Node): Robot {
    return RobotImpl(orientation, node)
}

private enum class Phase {
    MoveSouth, FindTile, MoveTile, Finished
}

class RobotImpl(orientation: Int, node: Node) : Robot(
    orientation = orientation,
    node = node,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0
) {
    private var phase = Phase.MoveSouth

    private var tilesToSides = false

    override fun activate() {
        when (phase) {
            Phase.MoveSouth -> moveSouth()
            Phase.FindTile -> findTile()
            Phase.MoveTile -> moveTile()
            Phase.Finished -> return
        }
    }

    override fun finished() = phase == Phase.Finished

    override fun getColor(): Color {
        return when (phase) {
            Phase.MoveSouth -> Color.ORANGE
            Phase.FindTile -> Color.TEAL
            Phase.MoveTile -> Color.SKY
            Phase.Finished -> Color.BLACK
        }
    }

    private fun moveSouth() {
        if (hasTileAtLabel(3)) {
            moveToLabel(3)
            return
        }
        tilesToSides = false
        phase = Phase.FindTile
    }

    private fun findTile() {
        for (label in arrayOf(0, 2, 3, 5)) {
            tilesToSides = tilesToSides || hasTileAtLabel(label)
        }
        for (label in arrayOf(0, 5, 4)) {
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                return
            }
        }
        if (!tilesToSides) {
            phase = Phase.Finished
            return
        }
        tilesToSides = false
        liftTile()
        moveToLabel(2)
        phase = Phase.MoveTile
    }

    private fun moveTile() {
        if (!isOnTile()) {
            placeTile()
            phase = Phase.FindTile
            return
        }
        moveToLabel(3)
    }
}