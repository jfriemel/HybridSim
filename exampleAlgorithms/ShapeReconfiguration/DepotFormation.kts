fun getRobot(
    node: Node,
    orientation: Int,
): Robot {
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

    /**
     * Enter phase: [Phase.MoveSouth]
     *
     * The robot moves to the southern end of its current column of tiles.
     *
     * Exit phase: [Phase.FindTile]
     */
    private fun moveSouth() {
        if (hasTileAtLabel(3)) {
            moveToLabel(3)
            return
        }
        tilesToSides = false
        phase = Phase.FindTile
    }

    /**
     * Enter phase: [Phase.FindTile]
     *
     * The robot moves to the northernmost westernmost tile and lifts it. During the movement, it
     * checks whether a line may already be formed. If so, it switches to the line moving phases.
     *
     * Exit phases: [Phase.MoveLineNorthWest] [Phase.MoveLineSouth] [Phase.MoveTile]
     */
    private fun findTile() {
        tilesToSides = tilesToSides || intArrayOf(1, 2, 4, 5).any(::hasTileAtLabel)
        containsTarget = containsTarget || isOnTarget()
        intArrayOf(5, 4, 0).firstOrNull(::hasTileAtLabel)?.let { label ->
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

    /**
     * Enter phase: [Phase.MoveTile]
     *
     * The robot places its carried tile below the southern tip of its tile column.
     *
     * Exit phase: [Phase.FindTile]
     */
    private fun moveTile() {
        if (!isOnTile()) {
            placeTile()
            phase = Phase.FindTile
            return
        }
        moveToLabel(3)
    }

    /**
     * Enter phase: [Phase.MoveLineNorthWest]
     *
     * The robot moves the entire line in direction north-west.
     *
     * Exit phase: [Phase.MoveLineSouth]
     */
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

    /**
     * Enter phase: [Phase.MoveLineSouth]
     *
     * The robot moves the entire line in direction south.
     *
     * Exit phase: [Phase.Finished]
     */
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
