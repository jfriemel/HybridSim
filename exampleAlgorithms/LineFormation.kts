/** Single-robot line formation algorithm from https://doi.org/10.1007/s11047-019-09774-2 */
fun getRobot(
    node: Node,
    orientation: Int,
): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    MoveSouth,
    FindTile,
    MoveTile,
    Finished,
}

class RobotImpl(node: Node, orientation: Int) :
    Robot(
        node = node,
        orientation = orientation,
        carriesTile = false,
        numPebbles = 0,
        maxPebbles = 0,
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
        tilesToSides = tilesToSides || intArrayOf(1, 2, 4, 5).any(::hasTileAtLabel)
        intArrayOf(5, 4, 0).firstOrNull(::hasTileAtLabel)?.let { label ->
            // Note: In the tile shape formation paper mentioned above, the precedence of movement
            // directions in the tile searching phase is given as N, NW, SW (0, 5, 4) instead of NW,
            // SW, N (5, 4, 0).
            // This is a mistake. In the proof of Theorem 3, it becomes clear that the authors mean
            // NW, SW, N, i.e., they want the robot to move as far west as possible before moving
            // north.
            moveToLabel(label)
            return
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
