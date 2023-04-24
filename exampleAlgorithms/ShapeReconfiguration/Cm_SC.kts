fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindOverhang, BuildOverhangBlock, LeaveOverhang, ExploreColumn, ExploreBoundary, ReturnToColumnTop, ReturnToBoundary
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0
) {
    private var phase = Phase.FindOverhang

    private var entryTile: Boolean = false
    private var moveLabel: Int = 0
    private var outerLabel: Int = -1
    private var blockDir: Int = -1
    private var enterLabel: Int = 0
    private var columnDir: Int = 0

    override fun activate() {
        when (phase) {
            Phase.FindOverhang -> findOverhang()
            Phase.BuildOverhangBlock -> findRemovableOverhang()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.ExploreColumn -> exploreColumn()
            Phase.ReturnToColumnTop -> returnToColumnTop()
            Phase.ExploreBoundary -> exploreBoundary()
            Phase.ReturnToBoundary -> returnToBoundary()
        }
    }

    override fun getColor(): Color {
        return when (phase) {  // BLUE, TEAL
            Phase.FindOverhang -> Color.ORANGE
            Phase.BuildOverhangBlock -> Color.SCARLET
            Phase.ExploreColumn -> Color.BLUE
            Phase.ReturnToColumnTop -> Color.TEAL
            Phase.ExploreBoundary -> Color.BROWN
            else -> Color.WHITE
        }
    }

    /**
     * Enter phase: [Phase.FindOverhang]
     *
     * The robot traverses the target tile boundary until it encounters an overhang tile.
     * It moves to the tile and sets [entryTile] to true.
     *
     * Exit phase: [Phase.BuildOverhangBlock]
     */
    private fun findOverhang() {
        if (hasOverhangNbr()) {
            val moveLabel = overhangNbrLabel()!!
            moveToLabel(moveLabel)
            entryTile = true
            phase = Phase.BuildOverhangBlock
            outerLabel = -1
            blockDir = (moveLabel + 3).mod(6)
            return
        }

        traverseTargetTileBoundary()
    }

    private fun buildOverhangBlock() {
        findRemovableOverhang()
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target tile, it instead
     * traverses the boundary of the entire tile structure until it encounters a target tile.
     */
    private fun traverseTargetTileBoundary() {
        if (outerLabel < 0) {
            if (hasOverhangNbr()) {
                outerLabel = overhangNbrLabel()!!
            } else if (hasEmptyNonTargetNbr()) {
                outerLabel = emptyNonTargetNbrLabel()!!
            } else {
                throw Exception("Robot at node $node is not at target tile boundary.")
            }
        }

        if (!isOnTarget() && hasTargetTileNbr()) {
            columnDir = targetTileNbrLabel()!!
            moveToLabel(columnDir)
            return
        }

        var moveLabel = (outerLabel + 1).mod(6)
        while (!canMoveToLabel(moveLabel) || !hasTileAtLabel(moveLabel) || (isOnTarget() && !labelIsTarget(moveLabel))) {
            outerLabel = moveLabel
            moveLabel = (outerLabel + 1).mod(6)
        }
        moveToLabel(moveLabel)
        outerLabel = (outerLabel - 1).mod(6)
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of a connected component of overhang tiles.
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
        moveLabel = (1..5).map { offset -> (firstInvalidLabel + offset).mod(6) }.first(isLabelValid)
        moveToLabel(moveLabel)
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at an edge of a connected component of overhang tiles where a tile can safely be
     * removed.
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

    private fun exploreColumn() {
        if (carriesTile && !isOnTile()) {
            placeTile()
            phase = Phase.ReturnToBoundary
            return
        }
        if (labelIsTarget(columnDir)) {
            moveAndUpdate(columnDir)
        } else {
            phase = Phase.ReturnToColumnTop
        }
    }

    private fun returnToColumnTop() {
        val revColumnDir = (columnDir + 3).mod(6)
        if (hasTileAtLabel(revColumnDir) && labelIsTarget(revColumnDir)) {
            moveAndUpdate(revColumnDir)
            return
        }
        outerLabel = revColumnDir
        if (!carriesTile) {
            phase = Phase.FindOverhang
            return
        }
        for (label in intArrayOf(1, 2, 3, 4, 5, 6).map { (outerLabel + it).mod(6) }) {
            if (labelIsTarget(label)) {
                moveAndUpdate(label)
                phase = Phase.ExploreBoundary
                return
            }
        }
        phase = Phase.ExploreColumn
    }

    private fun exploreBoundary() {
        if (!isOnTile()) {
            placeTile()
            phase = Phase.FindOverhang
            return
        }

        if ((0 <= enterLabel && enterLabel <= 2
                && !labelIsTarget((columnDir + 3).mod(6))
                && (enterLabel == 2 || !labelIsTarget((columnDir + 2).mod(6))))
            || ((enterLabel == 4 || enterLabel == 5) && intArrayOf(0, 1, 2, 3).all { offset ->
                !labelIsTarget((columnDir + offset).mod(6))
            })
        ) {
            phase = Phase.ExploreColumn
            return
        }
        var label = (enterLabel + columnDir).mod(6)
        repeat(6) {
            outerLabel = label
            label = (label + 1).mod(6)
            if (labelIsTarget(label)) {
                moveAndUpdate(label)
                outerLabel = (outerLabel - 1).mod(6)
                return
            }
        }
    }

    private fun returnToBoundary() {
        val revColumnDir = (columnDir + 3).mod(6)
        if ((hasTileAtLabel(revColumnDir) && !labelIsTarget(revColumnDir)) || !hasTileAtLabel(revColumnDir)) {
            outerLabel = revColumnDir
            phase = Phase.FindOverhang
            return
        }
        moveToLabel(revColumnDir)
    }

    private fun moveAndUpdate(label: Int) {
        moveToLabel(label)
        enterLabel = (label + 3 - columnDir).mod(6)
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
     * Exit phase: [Phase.LeaveOverhang] or [Phase.ExploreColumn]
     */
    private fun findRemovableOverhang() {
        if ((!entryTile && isAtOverhangEdge()) || !hasOverhangNbr()) {
            liftTile()
            phase = Phase.LeaveOverhang
            return
        }

        entryTile = false
        traverseOverhangBoundary()
    }

    private fun leaveOverhang() {
        if (isOnTarget()) {
            phase = Phase.ExploreColumn
            return
        }
        traverseTargetTileBoundary()
    }
}
