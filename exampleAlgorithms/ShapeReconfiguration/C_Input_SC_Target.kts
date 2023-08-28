fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    // Block formation for finding outer boundary
    FindBlockTile,
    MoveBlockTile,
    ReturnTile,

    // Compaction and search for removable overhang tile
    FindOverhang,
    CompactOverhang,
    CompactOverhangHelper,
    LeaveOverhang,

    // Exploration for finding demand node
    ExploreColumn,
    ReturnToColumnTop,
    ExploreBoundary,
}

class RobotImpl(node: Node, orientation: Int) :
    Robot(
        node = node,
        orientation = orientation,
        carriesTile = false,
        numPebbles = 0,
        maxPebbles = 0,
    ) {
    private var phase = Phase.FindBlockTile

    private var entryTile: Boolean = false
    private var outerLabel: Int = -1
    private var columnDir: Int = 0

    private var compactionDir: Int = -1
    private var hasMoved: Boolean = false

    private var initialTarget = false

    override fun activate() {
        when (phase) {
            Phase.FindBlockTile -> findBlockTile()
            Phase.MoveBlockTile -> moveBlockTile()
            Phase.ReturnTile -> returnTile()
            Phase.FindOverhang -> findOverhang()
            Phase.CompactOverhang -> compactOverhang()
            Phase.CompactOverhangHelper -> compactOverhangHelper()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.ExploreColumn -> exploreColumn()
            Phase.ReturnToColumnTop -> returnToColumnTop()
            Phase.ExploreBoundary -> exploreBoundary()
        }
    }

    override fun getColor(): Color =
        when (phase) {
            Phase.FindBlockTile,
            Phase.MoveBlockTile,
            Phase.ReturnTile -> Color.ORANGE
            Phase.FindOverhang,
            Phase.CompactOverhang,
            Phase.CompactOverhangHelper,
            Phase.LeaveOverhang -> Color.SCARLET
            Phase.ExploreColumn,
            Phase.ReturnToColumnTop,
            Phase.ExploreBoundary -> Color.SKY
        }

    /**
     * Enter phase: [Phase.FindBlockTile]
     *
     * Forms a block until it finds the boundary of the target shape.
     *
     * Exit phases: [Phase.FindOverhang] [Phase.MoveBlockTile]
     */
    private fun findBlockTile() {
        initialTarget = isOnTarget()

        // If at outer boundary, move to target tile and enter [Phase.FindOverhang]
        if (isOnTarget() && (hasOverhangNbr() || hasEmptyNonTargetNbr())) {
            outerLabel = overhangNbrLabel() ?: emptyNonTargetNbrLabel()!!
            phase = Phase.FindOverhang
            return
        } else if (!isOnTarget() && hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            phase = Phase.FindOverhang
            return
        }

        // If possible, move in direction 5, 4, or 0
        intArrayOf(5, 4, 0)
            .firstOrNull { label -> hasTileAtLabel(label) }
            ?.let { label ->
                moveToLabel(label)
                return
            }

        // Cannot move further, lift tile and enter [Phase.BlockMoveTile]
        liftTile()
        phase = Phase.MoveBlockTile
    }

    /**
     * Enter phase: [Phase.MoveBlockTile]
     *
     * Moves a tile southeast until it encounters a non-occupied node in the block formation
     * algorithm.
     *
     * Exit phases: [Phase.FindBlockTile] [Phase.ReturnTile]
     */
    private fun moveBlockTile() {
        moveToLabel(2)
        if (!isOnTile()) {
            if (!initialTarget && isOnTarget()) {
                phase = Phase.ReturnTile
            } else {
                placeTile()
                phase = Phase.FindBlockTile
            }
        }
    }

    /**
     * Enter phase: [Phase.ReturnTile]
     *
     * Moves a tiles northwest to avoid placing it on a demand node not connected to the target tile
     * shape. Then moves back southwest until it encounters a demand node to enter the target tile
     * shape.
     *
     * Exit phases: [Phase.FindOverhang] [Phase.LeaveOverhang]
     */
    private fun returnTile() {
        if (carriesTile && !isOnTile() && !isOnTarget()) {
            placeTile()
            return
        } else if (!carriesTile && hasTargetTileNbr()) {
            moveToLabel(targetTileNbrLabel()!!)
            phase = Phase.FindOverhang
            return
        } else if (!carriesTile && hasEmptyTargetNbr()) {
            outerLabel = emptyTargetNbrLabel()!!
            phase = Phase.LeaveOverhang
            return
        }

        if (carriesTile) {
            moveToLabel(5)
        } else {
            moveToLabel(2)
        }
    }

    /**
     * Enter phase: [Phase.FindOverhang]
     *
     * The robot traverses the target tile boundary until it encounters an overhang tile. It moves
     * to the tile and sets [entryTile] to true.
     *
     * Exit phase: [Phase.CompactOverhang]
     */
    private fun findOverhang() {
        if (hasOverhangNbr()) {
            val moveLabel = overhangNbrLabel()!!
            moveToLabel(moveLabel)
            entryTile = true
            outerLabel = (moveLabel + 3).mod(6)
            phase = Phase.CompactOverhang
            return
        }

        traverseTargetTileBoundary()
    }

    /**
     * Enter phase: [Phase.CompactOverhang]
     *
     * The robot moves along the overhang boundary (induced by [outerLabel]) and moves tiles away
     * from the boundary (inward if the overhang component is not simply connected), or picks up
     * safely removable tiles.
     *
     * Exit phases: [Phase.LeaveOverhang] [Phase.CompactOverhangHelper]
     */
    private fun compactOverhang() {
        if (carriesTile && !isOnTile()) {
            placeTile()
            entryTile = true
            return
        }

        if (
            !hasOverhangNbr() || ((!entryTile || !hasTargetTileNbr()) && isOnTile() && isAtBorder())
        ) {
            liftTile()
            phase = Phase.LeaveOverhang
            return
        }

        val validLabel = { label: Int -> hasTileAtLabel(label) && !labelIsTarget(label) }
        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first(validLabel)

        if (
            !entryTile &&
                !hasTileAtLabel((moveLabel + 1).mod(6)) &&
                !labelIsTarget((moveLabel + 1).mod(6)) &&
                hasTileAtLabel((moveLabel + 2).mod(6)) &&
                !labelIsTarget((moveLabel + 2).mod(6)) &&
                intArrayOf(3, 4, 5).all { offset ->
                    !hasTileAtLabel((moveLabel + offset).mod(6)) ||
                        labelIsTarget((moveLabel + offset).mod(6))
                }
        ) {
            liftTile()
            compactionDir = (moveLabel + 1).mod(6)
            hasMoved = false
            phase = Phase.CompactOverhangHelper
            return
        }

        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
        entryTile = false
    }

    /**
     * Enter phase: [Phase.CompactOverhangHelper]
     *
     * The robot moves the picked up tile inward (toward [compactionDir]) and then moves to its
     * successor tile in its boundary traversal.
     *
     * Exit phase: [Phase.CompactOverhang]
     */
    private fun compactOverhangHelper() {
        if (!hasMoved) {
            moveToLabel(compactionDir)
            hasMoved = true
            return
        }
        if (carriesTile) {
            val invalidNbrs =
                labels.filter { label -> !hasTileAtLabel(label) || labelIsTarget(label) }
            if (
                (hasTileAtLabel(compactionDir) && !labelIsTarget(compactionDir)) &&
                    (invalidNbrs.size > 2 && (compactionDir + 1).mod(6) in invalidNbrs)
            ) {
                moveToLabel((compactionDir + 3).mod(6))
                phase = Phase.CompactOverhang
            } else {
                placeTile()
            }
            return
        }

        moveToLabel((compactionDir + 4).mod(6))
        outerLabel = (compactionDir + 2).mod(6)
        phase = Phase.CompactOverhang
    }

    /**
     * Enter phase: [Phase.LeaveOverhang]
     *
     * The robot has picked up a tile and moves it along the boundary of the overhang component
     * until it reaches the boundary of the target tile structure.
     *
     * Exit phase: [Phase.ExploreColumn]
     */
    private fun leaveOverhang() {
        if (hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            columnDir = moveLabel
            phase =
                if (carriesTile) {
                    Phase.ExploreColumn
                } else {
                    Phase.FindOverhang
                }
            return
        }
        val moveLabel =
            (1..6)
                .map { (outerLabel + it).mod(6) }
                .first { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Enter phase: [Phase.ExploreColumn]
     *
     * The robot moves along a column (direction specified by [columnDir]) of target nodes until it
     * either reaches the column's end (a non-target node) or a demand node where it can place the
     * tile it is carrying.
     *
     * Exit phase: [Phase.ReturnToColumnTop]
     */
    private fun exploreColumn() {
        // Place tile if demand node on column
        if (carriesTile && !isOnTile()) {
            placeTile()
            phase = Phase.ReturnToColumnTop
            return
        }

        // Move along column as long as possible, then turn around
        if (labelIsTarget(columnDir)) {
            moveToLabel(columnDir)
        } else {
            phase = Phase.ReturnToColumnTop
        }
    }

    /**
     * Enter phase: [Phase.ReturnToColumnTop]
     *
     * The robot moves back up the column (opposite direction of [columnDir]) until it reaches the
     * column's end. Then it moves to the next tile along the boundary (unless the target tile
     * structure only consists of one column).
     *
     * Exit phases: [Phase.ExploreBoundary] [Phase.FindOverhang]
     */
    private fun returnToColumnTop() {
        val revColumnDir = (columnDir + 3).mod(6)
        if (hasTileAtLabel(revColumnDir) && labelIsTarget(revColumnDir)) {
            moveToLabel(revColumnDir)
            return
        }

        if (!carriesTile) {
            phase = Phase.FindOverhang
            return
        }

        (1..6)
            .map { (outerLabel + it).mod(6) }
            .firstOrNull { label -> labelIsTarget(label) }
            ?.let { label ->
                moveToLabel(label)
                outerLabel = (label - 2).mod(6)
            }

        phase = Phase.ExploreBoundary
    }

    /**
     * Enter phase: [Phase.ExploreBoundary]
     *
     * The robot moves along the boundary of the target shape and triggers column searches or places
     * its tile when possible.
     *
     * Exit phases: [Phase.FindOverhang] [Phase.ExploreColumn]
     */
    private fun exploreBoundary() {
        if (!isOnTile()) {
            placeTile()
            phase = Phase.FindOverhang
            return
        }

        if (!labelIsTarget((columnDir + 3).mod(6))) {
            phase = Phase.ExploreColumn
            return
        }

        val moveLabel =
            (1..6)
                .map { offset -> (outerLabel + offset).mod(6) }
                .first { label -> labelIsTarget(label) }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target
     * tile, it instead traverses the boundary of the entire tile structure until it encounters a
     * target tile.
     */
    private fun traverseTargetTileBoundary() {
        if (!isOnTarget() && hasTargetTileNbr()) {
            columnDir = targetTileNbrLabel()!!
            moveToLabel(columnDir)
            return
        }

        val moveLabel =
            (1..6)
                .map { (outerLabel + it).mod(6) }
                .firstOrNull { label ->
                    canMoveToLabel(label) &&
                        hasTileAtLabel(label) &&
                        (labelIsTarget(label) || !isOnTarget())
                }
                ?: return
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of a connected component of either overhang or target
     * tiles where a tile can safely be removed.
     */
    private fun isAtBorder(): Boolean {
        val boundaryLabels =
            labels.filter { label ->
                (labelIsTarget(label) != isOnTarget()) || !hasTileAtLabel(label)
            }

        if (boundaryLabels.size == 6) {
            return true
        }

        return boundaryLabels.filter { label -> (label + 1).mod(6) !in boundaryLabels }.size == 1
    }
}
