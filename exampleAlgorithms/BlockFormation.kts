// Single-robot block formation algorithm from https://doi.org/10.1007/s11047-019-09774-2

fun getRobot(
    node: Node,
    orientation: Int,
): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
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
    private var phase = Phase.FindTile

    private var testActive = false
    private var numMovementsSW = 0
    private var southernmost = false

    override fun activate() {
        when (phase) {
            Phase.FindTile -> findTile()
            Phase.MoveTile -> moveTile()
            Phase.Finished -> return
        }
    }

    override fun finished() = phase == Phase.Finished

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindTile -> Color.ORANGE
            Phase.MoveTile -> Color.TEAL
            Phase.Finished -> Color.BLACK
        }
    }

    private fun findTile() {
        intArrayOf(5, 4, 0)
            .firstOrNull(::hasTileAtLabel)
            ?.let { label ->
                moveToLabel(label)
                if (label == 4) {
                    numMovementsSW++
                }
                return
            }

        liftTile()
        if (!hasTileAtLabel(1) && !testActive) {
            testActive = true
            southernmost = false
        } else if (!hasTileAtLabel(3)) {
            southernmost = true
        }
        phase = Phase.MoveTile
    }

    private fun moveTile() {
        moveToLabel(2)
        if (
            (hasTileAtLabel(1) && !hasTileAtLabel(0)) || (hasTileAtLabel(3) && !hasTileAtLabel(4))
        ) {
            testActive = false
        }
        if (!isOnTile()) {
            placeTile()
            if (hasTileAtLabel(2) || numMovementsSW > 1) {
                testActive = false
            } else if (testActive && southernmost) {
                phase = Phase.Finished
                return
            }
            numMovementsSW = 0
            phase = Phase.FindTile
            return
        }
    }
}
