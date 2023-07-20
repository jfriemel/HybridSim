fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindBoundary,
    LeaveOverhang,
    FindOverhang,
    FindRemovableOverhang,
    FindDemandComponent,
    PlaceTargetTile,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = 0,  // The robots share a compass
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.FindBoundary

    private var entryTile: Boolean = false
    private var moveLabel: Int = 0

    val successorDirs = arrayOf(false, false, false, false, false, false)

    override fun activate() {
        when (phase) {
            Phase.FindBoundary -> findBoundary()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.FindOverhang -> findOverhang()
            Phase.FindRemovableOverhang -> findRemovableOverhang()
            Phase.FindDemandComponent -> findDemandComponent()
            Phase.PlaceTargetTile -> placeTargetTile()
        }
        updateSuccessorDirs()
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindBoundary -> Color.TEAL
            Phase.LeaveOverhang -> Color.YELLOW
            Phase.FindOverhang -> Color.ORANGE
            Phase.FindRemovableOverhang -> Color.SCARLET
            Phase.FindDemandComponent -> Color.SKY
            Phase.PlaceTargetTile -> Color.WHITE
        }
    }

    /**
     * Enter phase: [Phase.FindBoundary]
     *
     * The robot walks north until it reaches the boundary of the input shape.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.LeaveOverhang]
     */
    private fun findBoundary() {
        if (!isAtBoundary()) {
            moveToLabel(0)
        } else if (isOnTarget()) {
            phase = Phase.FindOverhang
        } else {
            phase = Phase.LeaveOverhang
        }
    }

    /**
     * Enter phases:
     *   [Phase.FindBoundary]
     *   [Phase.FindRemovableOverhang]
     *
     * The robot traverses the boundary of the tile shape until it is no longer on an overhang.
     *
     * Exit phase: [Phase.FindOverhang]
     */
    private fun leaveOverhang() {
        if (!isOnTarget()) {
            traverseTargetTileBoundary()
        } else {
            phase = Phase.FindOverhang
        }
    }

    /**
     * Enter phase: [Phase.FindOverhang]
     *
     * The robot traverses the target tile boundary until it encounters an overhang tile.
     * It moves to the tile and sets [entryTile] to true.
     *
     * Exit phase: [Phase.FindRemovableOverhang]
     */
    private fun findOverhang() {
        if (hasOverhangNbr()) {
            if (moveToLabel(overhangNbrLabel()!!)) {
                entryTile = true
                phase = Phase.FindRemovableOverhang
            }
            return
        }

        traverseTargetTileBoundary()
    }

    /**
     * Enter phase: [Phase.FindRemovableOverhang]
     *
     * The robot traverses the overhang boundary until it encounters a tile that can be removed without breaking the
     * connectivity of the overhang component (checked by [isAtOverhangBorder]).
     * To make sure that the robot does not disconnect the overhang component from the rest of the tile structure, it
     * does not lift the first overhang tile it moved to (checked by [entryTile]) unless it is the only tile of the
     * component.
     *
     * Exit phase: [Phase.FindDemandComponent]
     */
    private fun findRemovableOverhang() {
        if (hasHangingRobotNbr()) {
            phase = Phase.LeaveOverhang
            return
        }

        if ((!entryTile && isAtOverhangBorder()) || !hasOverhangNbr()) {
            liftTile()
            phase = Phase.FindDemandComponent
            return
        }

        if (traverseOverhangBoundary()) {
            entryTile = false
        }
    }

    /**
     * Enter phase: [Phase.FindDemandComponent]
     *
     * The robot traverses the target tile boundary until it can move to demand node.
     *
     * Exit phase: [Phase.PlaceTargetTile]
     */
    private fun findDemandComponent() {
        if (isOnTile() && isOnTarget() && hasEmptyTargetNbr()) {
            if (moveToLabel(emptyTargetNbrLabel()!!)) {
                phase = Phase.PlaceTargetTile
            }
            return
        }

        traverseTargetTileBoundary()
    }

    /**
     * Enter phase: [Phase.PlaceTargetTile]
     *
     * The robot traverses the boundary of the connected component of demand nodes until it reaches a border where it
     * places the tile it is carrying without creating a hole in the intermediate target shape.
     *
     * Exit phase: [Phase.FindOverhang]
     */
    private fun placeTargetTile() {
        if (isAtDemandBorder()) {
            placeTile()
            phase = Phase.FindOverhang
            return
        }

        traverseDemandBoundary()
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target tile, it instead
     * traverses the boundary of the entire tile structure until it encounters a target tile.
     * @return True if the robot has moved, false otherwise.
     */
    private fun traverseTargetTileBoundary(): Boolean {
        if (!isOnTarget() && hasTargetTileNbr()) {
            return moveToLabel(targetTileNbrLabel()!!)
        }

        return traverseBoundary { label ->
            hasTileAtLabel(label) && (!isOnTarget() || labelIsTarget(label))
        }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of an overhang component.
     * @return True if the robot has moved, false otherwise.
     */
    private fun traverseOverhangBoundary() = traverseBoundary { label ->
        hasTileAtLabel(label) && !labelIsTarget(label)
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of a connected component of demand nodes.
     * @return True if the robot has moved, false otherwise.
     */
    private fun traverseDemandBoundary(): Boolean = traverseBoundary { label ->
        (hasTileAtLabel((label - 1).mod(6)) || hasTileAtLabel((label + 1).mod(6)))
            && !hasTileAtLabel(label) && labelIsTarget(label)
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of nodes whose labels are considered valid by [isLabelValid].
     * @return True if the robot has moved, false otherwise.
     */
    fun traverseBoundary(isLabelValid: (Int) -> Boolean): Boolean {
        val firstInvalidLabel = (1..6).map { (moveLabel - it).mod(6) }
            .firstOrNull { label -> !isLabelValid(label) } ?: (moveLabel - 1).mod(6)
        moveLabel = (1..5).map { offset ->
            (firstInvalidLabel + offset).mod(6)
        }.firstOrNull(isLabelValid) ?: return false
        return moveToLabel(moveLabel)
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of an overhang component where a tile can safely be removed.
     */
    private fun isAtOverhangBorder(): Boolean = isAtBorder { label -> labelIsTarget(label) || !hasTileAtLabel(label) }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of a connected component of demand nodes where a tile can safely be
     * placed.
     */
    private fun isAtDemandBorder(): Boolean = isAtBorder { label -> !(hasTileAtLabel(label) && labelIsTarget(label)) }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of a structure whose boundary labels are induced by [isLabelBoundary].
     */
    private fun isAtBorder(isLabelBoundary: (Int) -> Boolean): Boolean {
        val boundaryLabels = labels.filter(isLabelBoundary)

        if (boundaryLabels.size == 6) {
            return true
        }

        return boundaryLabels.filter { label -> (label + 1).mod(6) !in boundaryLabels }.size == 1
    }

    private fun updateSuccessorDirs() {
        successorDirs.fill(false)
        labels.forEach { label ->
            if (hasRobotAtLabel(label) && (robotAtLabel(label)!! as RobotImpl).successorDirs[(label + 3).mod(6)]) {

            }
        }
        successorDirs[moveLabel] = true
    }
}
