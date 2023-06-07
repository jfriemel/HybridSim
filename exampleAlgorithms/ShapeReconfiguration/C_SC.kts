fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    // Block formation for finding outer boundary
    FindBlockTile,
    MoveBlockTile,

    // Compression and search for removable overhang tile
    FindOverhang,
    SearchAndLiftOverhang,
    CompressOverhang,
    LeaveOverhang,

    // Exploration for finding demand node
    ExploreColumn,
    ReturnToColumnTop,
    ExploreBoundary,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
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

    private var compressDir: Int = -1
    private var hasMoved: Boolean = false

    override fun activate() {
        when (phase) {
            Phase.FindBlockTile -> findBlockTile()
            Phase.MoveBlockTile -> moveBlockTile()
            Phase.FindOverhang -> findOverhang()
            Phase.SearchAndLiftOverhang -> searchAndLiftOverhang()
            Phase.CompressOverhang -> compressOverhang()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.ExploreColumn -> exploreColumn()
            Phase.ReturnToColumnTop -> returnToColumnTop()
            Phase.ExploreBoundary -> exploreBoundary()
        }
    }

    override fun getColor(): Color = when (phase) {
        Phase.FindBlockTile, Phase.MoveBlockTile
            -> Color.ORANGE
        Phase.FindOverhang, Phase.SearchAndLiftOverhang, Phase.CompressOverhang, Phase.LeaveOverhang
            -> Color.SCARLET
        Phase.ExploreColumn, Phase.ReturnToColumnTop, Phase.ExploreBoundary
            -> Color.SKY
    }

    /**
     * Enter phase: [Phase.FindBlockTile]
     *
     * Forms a block until it finds the outer boundary of the target shape.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.MoveBlockTile]
     */
    private fun findBlockTile() {
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
        intArrayOf(5, 4, 0).firstOrNull { label -> hasTileAtLabel(label) }?.let { label ->
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
     * Moves a tile southeast until it encounters an empty node in the block formation algorithm.
     *
     * Exit phase: [Phase.FindBlockTile]
     */
    private fun moveBlockTile() {
        moveToLabel(2)
        if (!isOnTile()) {
            placeTile()
            phase = Phase.FindBlockTile
        }
    }

    /**
     * Enter phase: [Phase.FindOverhang]
     *
     * The robot traverses the target tile boundary until it encounters an overhang tile.
     * It moves to the tile and sets [entryTile] to true.
     *
     * Exit phase: [Phase.SearchAndLiftOverhang]
     */
    private fun findOverhang() {
        if (hasOverhangNbr()) {
            val moveLabel = overhangNbrLabel()!!
            moveToLabel(moveLabel)
            entryTile = true
            outerLabel = (moveLabel + 3).mod(6)
            phase = Phase.SearchAndLiftOverhang
            return
        }

        traverseTargetTileBoundary()
    }

    /**
     * Enter phase: [Phase.SearchAndLiftOverhang]
     *
     * The robot moves along the outer overhang boundary (induced by [outerLabel]) and moves tiles towards the inner
     * boundary, if one exists, or picks up safely removable tiles.
     *
     * Exit phases:
     *   [Phase.LeaveOverhang]
     *   [Phase.CompressOverhang]
     */
    private fun searchAndLiftOverhang() {
        if (!hasOverhangNbr() || ((!entryTile || !hasTargetTileNbr()) && isOnTile() && isAtBorder())) {
            liftTile()
            phase = Phase.LeaveOverhang
            return
        }

        val validLabel = { label: Int -> hasTileAtLabel(label) && !labelIsTarget(label) }
        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first(validLabel)

        if (
            (!entryTile || !hasTargetTileNbr())
            && !hasTileAtLabel((moveLabel + 1).mod(6)) && !labelIsTarget((moveLabel + 1).mod(6))
            && validLabel((moveLabel + 2).mod(6))
            && (validLabel((moveLabel + 3).mod(6))
                || !hasTileAtLabel((moveLabel + 4).mod(6)) && !labelIsTarget((moveLabel + 4).mod(6)))
        ) {
            liftTile()
            compressDir = (moveLabel + 1).mod(6)
            hasMoved = false
            phase = Phase.CompressOverhang
            return
        }

        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
        entryTile = false
    }

    /**
     * Enter phase: [Phase.CompressOverhang]
     *
     * The robot moves the picked up tile towards the inner boundary and then returns to the tile before its previous
     * position at the outer boundary.
     *
     * Exit phase: [Phase.SearchAndLiftOverhang]
     */
    private fun compressOverhang() {
        if (!hasMoved) {
            moveToLabel(compressDir)
            hasMoved = true
            return
        }
        if (carriesTile) {
            placeTile()
            return
        }
        moveToLabel((compressDir + 4).mod(6))
        outerLabel = (compressDir + 2).mod(6)
        phase = Phase.SearchAndLiftOverhang
    }

    /**
     * Enter phase: [Phase.LeaveOverhang]
     *
     * The robot has picked up a tile and moves it along the outer boundary of the overhang component until it reaches
     * the outer boundary of the target tile structure.
     *
     * Exit phase: [Phase.ExploreColumn]
     */
    private fun leaveOverhang() {
        if (hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            columnDir = moveLabel
            phase = Phase.ExploreColumn
            return
        }
        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Enter phase: [Phase.ExploreColumn]
     *
     * The robot moves along a column (direction specified by [columnDir]) of target nodes until it either reaches the
     * column's end (a non-target node) or a demand node where it can place the tile it is carrying.
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
     * The robot moves back up the column (opposite direction of [columnDir]) until it reaches the column's end.
     * Then it moves to the next tile along the boundary (unless the target tile structure only consists of one column).
     *
     * Exit phases:
     *   [Phase.ExploreBoundary]
     *   [Phase.FindOverhang]
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

        (1..6).map { (outerLabel + it).mod(6) }.firstOrNull { label -> labelIsTarget(label) }?.let { label ->
            moveToLabel(label)
            outerLabel = (label - 2).mod(6)
        }

        phase = Phase.ExploreBoundary
    }

    /**
     * Enter phase: [Phase.ExploreBoundary]
     *
     * The robot moves along the boundary of the target shape and triggers column searches or places its tile when
     * possible.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.ExploreColumn]
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

        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target tile, it instead
     * traverses the boundary of the entire tile structure until it encounters a target tile.
     */
    private fun traverseTargetTileBoundary() {
        if (!isOnTarget() && hasTargetTileNbr()) {
            columnDir = targetTileNbrLabel()!!
            moveToLabel(columnDir)
            return
        }

        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }
            .firstOrNull { label ->
                canMoveToLabel(label) && hasTileAtLabel(label) && (labelIsTarget(label) || !isOnTarget())
            } ?: return
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of a connected component of either overhang or targettiles where a tile
     * can safely be removed.
     */
    private fun isAtBorder(): Boolean {
        val boundaryLabels = labels.filter { label -> (labelIsTarget(label) != isOnTarget()) || !hasTileAtLabel(label) }

        if (boundaryLabels.size == 6) {
            return true
        }

        return boundaryLabels.filter { label -> (label + 1).mod(6) !in boundaryLabels }.size == 1
    }
}