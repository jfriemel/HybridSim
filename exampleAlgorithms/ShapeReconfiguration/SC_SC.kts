fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindBoundary,
    FindTargetOnBoundary,
    FindOverhang,
    FindRemovableOverhang,
    FindEmptyTarget,
    PlaceTargetTile,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.FindBoundary

    private var entryTile: Boolean = false
    private var moveLabel: Int = 0

    override fun activate() {
        when (phase) {
            Phase.FindBoundary -> findBoundary()
            Phase.FindTargetOnBoundary -> findTargetOnBoundary()
            Phase.FindOverhang -> findOverhang()
            Phase.FindRemovableOverhang -> findRemovableOverhang()
            Phase.FindEmptyTarget -> findEmptyTarget()
            Phase.PlaceTargetTile -> placeTargetTile()
        }
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindBoundary -> Color.BLUE
            Phase.FindTargetOnBoundary -> Color.TEAL
            Phase.FindOverhang -> Color.ORANGE
            Phase.FindRemovableOverhang -> Color.SCARLET
            Phase.FindEmptyTarget -> Color.SKY
            Phase.PlaceTargetTile -> Color.WHITE
        }
    }

    /**
     * Enter phase: [Phase.FindBoundary]
     *
     * The robot walks north until it reaches the boundary of the input shape.
     *
     * Exit phase: [Phase.FindTargetOnBoundary]
     */
    private fun findBoundary() {
        if (isAtBoundary()) {
            phase = Phase.FindTargetOnBoundary
            return
        }

        moveToLabel(0)
    }

    /**
     * Enter phase: [Phase.FindTargetOnBoundary]
     *
     * The robot traverses the boundary until it stands on a target tile.
     *
     * Exit phase: [Phase.FindOverhang]
     */
    private fun findTargetOnBoundary() {
        if (isOnTarget()) {
            phase = Phase.FindOverhang
            return
        }

        traverseTargetTileBoundary()
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
            moveToLabel(overhangNbrLabel()!!)
            entryTile = true
            phase = Phase.FindRemovableOverhang
            return
        }

        traverseTargetTileBoundary()
    }

    /**
     * Enter phase: [Phase.FindRemovableOverhang]
     *
     * The robot traverses the overhang boundary until it encounters a tile that can be removed without breaking the
     * connectivity of the overhang component (checked by [isAtOverhangEdge]).
     * To make sure that the robot does not disconnect the overhang component from the rest of the tile structure, it
     * does not lift the first overhang tile it moved to (checked by [entryTile]) unless it is the only tile of the
     * component.
     *
     * Exit phase: [Phase.FindEmptyTarget]
     */
    private fun findRemovableOverhang() {
        if ((!entryTile && isAtOverhangEdge()) || !hasOverhangNbr()) {
            liftTile()
            phase = Phase.FindEmptyTarget
            return
        }

        entryTile = false
        traverseOverhangBoundary()
    }

    /**
     * Enter phase: [Phase.FindEmptyTarget]
     *
     * The robot traverses the target tile boundary until it can move to an empty target node.
     *
     * Exit phase: [Phase.PlaceTargetTile]
     */
    private fun findEmptyTarget() {
        if (isOnTile() && isOnTarget() && hasEmptyTargetNbr()) {
            moveToLabel(emptyTargetNbrLabel()!!)
            phase = Phase.PlaceTargetTile
            return
        }

        traverseTargetTileBoundary()
    }

    /**
     * Enter phase: [Phase.PlaceTargetTile]
     *
     * The robot traverses the boundary of the connected component of empty target nodes until it reaches an edge where
     * it places the tile it is carrying without creating a hole in the intermediate target shape.
     *
     * Exit phase: [Phase.FindOverhang]
     */
    private fun placeTargetTile() {
        if (isAtEmptyTargetEdge()) {
            placeTile()
            phase = Phase.FindOverhang
            return
        }

        traverseEmptyTargetBoundary()
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target tile, it instead
     * traverses the boundary of the entire tile structure until it encounters a target tile.
     */
    private fun traverseTargetTileBoundary() {
        if (!isOnTarget() && hasTargetTileNbr()) {
            moveToLabel(targetTileNbrLabel()!!)
            return
        }

        traverseBoundary { label: Int ->
            canMoveToLabel(label) && hasTileAtLabel(label) && (!isOnTarget() || labelIsTarget(label))
        }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of an overhang component.
     */
    private fun traverseOverhangBoundary() {
        traverseBoundary { label: Int -> canMoveToLabel(label) && hasTileAtLabel(label) && !labelIsTarget(label) }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of a connected component of empty target nodes.
     */
    private fun traverseEmptyTargetBoundary() {
        traverseBoundary { label: Int -> canMoveToLabel(label) && !hasTileAtLabel(label) && labelIsTarget(label) }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of nodes whose labels are considered valid by [isLabelValid].
     */
    fun traverseBoundary(isLabelValid: (Int) -> Boolean) {
        val firstInvalidLabel = (1..6).map { (moveLabel - it).mod(6) }
            .firstOrNull { label -> !isLabelValid(label) } ?: (moveLabel - 1).mod(6)
        moveLabel = (1..5).map { offset -> (firstInvalidLabel + offset).mod(6) }.firstOrNull(isLabelValid) ?: return
        moveToLabel(moveLabel)
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at an edge of an overhang component where a tile can safely be removed.
     */
    private fun isAtOverhangEdge(): Boolean = isAtEdge { label -> labelIsTarget(label) || !hasTileAtLabel(label) }

    /**
     * Helper function
     *
     * Checks whether the robot is at an edge of a connected component of empty target nodes where a tile can safely be
     * placed.
     */
    private fun isAtEmptyTargetEdge(): Boolean = isAtEdge { label -> !(hasTileAtLabel(label) && labelIsTarget(label)) }

    /**
     * Helper function
     *
     * Checks whether the robot is at an edge of a structure whose boundary labels are induced by [isLabelBoundary].
     */
    private fun isAtEdge(isLabelBoundary: (Int) -> Boolean): Boolean {
        val boundaryLabels = labels.filter(isLabelBoundary)

        if (boundaryLabels.size == 6) {
            return true
        }

        var numBoundaries = 0
        boundaryLabels.forEach { label ->
            if ((label + 1).mod(6) !in boundaryLabels) {
                numBoundaries++
            }
        }
        return numBoundaries == 1
    }
}
