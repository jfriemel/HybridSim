/** Single-robot line formation algorithm from https://doi.org/10.1007/s11047-019-09774-2 */

fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    // Form line
    MoveSouth,
    FindTile,
    MoveTile,

    // Move line to southern tip of target shape
    MoveLineNorthWest,
    MoveLineSouth,

    // Finished
    Finished,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = 0,//orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.MoveSouth

    private var tilesToSides = false
    private var containsTarget = false

    override fun activate() {
        when (phase) {
            Phase.MoveSouth -> moveSouth()
            Phase.FindTile -> findTile()
            Phase.MoveTile -> moveTile()
            Phase.MoveLineNorthWest -> moveLineNorthWest()
            Phase.MoveLineSouth -> moveLineSouth()
            Phase.Finished -> return
        }
    }

    override fun finished() = phase == Phase.Finished

    override fun getColor(): Color {
        return when (phase) {
            Phase.MoveSouth -> Color.ORANGE
            Phase.FindTile -> Color.TEAL
            Phase.MoveTile -> Color.SKY
            Phase.MoveLineNorthWest -> Color.YELLOW
            Phase.MoveLineSouth -> Color.SCARLET
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
        tilesToSides = tilesToSides || intArrayOf(1, 2, 4, 5).any { label -> hasTileAtLabel(label) }
        containsTarget = containsTarget || isOnTarget()
        intArrayOf(5, 4, 0).firstOrNull() { label -> hasTileAtLabel(label) }?.let { label ->
            moveToLabel(label)
            return
        }

        if (!tilesToSides) {
            phase = if (containsTarget) Phase.MoveLineSouth else Phase.MoveLineNorthWest
            return
        }

        tilesToSides = false
        containsTarget = false
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

    private fun moveLineNorthWest() {
        if (!carriesTile) {
            if (hasTileAtLabel(1)) {
                moveToLabel(1)
            } else if (hasTileAtLabel(3)) {
                moveToLabel(3)
            } else {
                liftTile()
            }
            return
        }

        if (!hasTileAtLabel(1) && !hasTileAtLabel(2) && !hasTileAtLabel(3)) {
            moveToLabel(5)
            return
        }

        containsTarget = containsTarget || isOnTarget()
        if (isOnTile()) {
            moveToLabel(0)
            return
        }
        placeTile()
        if (!hasTileAtLabel(1) && containsTarget) {
            phase = Phase.MoveLineSouth
        }
    }

    private fun moveLineSouth() {
        if (!carriesTile) {
            if (hasTileAtLabel(0)) {
                moveToLabel(0)
            } else {
                liftTile()
                containsTarget = false
            }
            return
        }
        if (isOnTile() || hasTileAtLabel(3)) {
            moveToLabel(3)
            containsTarget = containsTarget || isOnTarget()
            return
        }
        placeTile()
        if (!containsTarget) {
            phase = Phase.Finished
        }
        containsTarget = false
    }
}
