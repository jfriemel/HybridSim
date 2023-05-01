fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindOverhang,
    FindRemovableOverhang,
    PlaceTileOnTarget,
}

private enum class SubPhase {
    // Block formation in overhang component
    SearchAndLiftOverhang,
    CompressOverhang,
    LeaveOverhang,

    // Exploration for finding target tile
    ExploreColumn,
    ReturnToColumnTop,
    ExploreBoundary,
    ReturnToBoundary,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.FindOverhang
    private var subPhase = SubPhase.ExploreColumn

    private var entryTile: Boolean = false
    private var moveLabel: Int = 0
    private var outerLabel: Int = -1
    private var enterAngle: Int = 0
    private var columnDir: Int = 0

    private var compressDir: Int = -1
    private var hasMoved: Boolean = false

    override fun activate() {
        when (phase) {
            Phase.FindOverhang -> findOverhang()
            Phase.FindRemovableOverhang -> when (subPhase) {
                SubPhase.SearchAndLiftOverhang -> searchAndLiftOverhang()
                SubPhase.CompressOverhang -> compressOverhang()
                SubPhase.LeaveOverhang -> leaveOverhang()
                else -> throw Exception("Incompatible phases: Phase $phase with sub-phase $subPhase")
            }
            Phase.PlaceTileOnTarget -> when (subPhase) {
                SubPhase.ExploreColumn -> exploreColumn()
                SubPhase.ReturnToColumnTop -> returnToColumnTop()
                SubPhase.ExploreBoundary -> exploreBoundary()
                SubPhase.ReturnToBoundary -> returnToBoundary()
                else -> throw Exception("Incompatible phases: Phase $phase with sub-phase $subPhase")
            }
        }
    }

    override fun getColor(): Color {
        return when (phase) {  // Available: [Color.BROWN]
            Phase.FindOverhang -> Color.ORANGE
            Phase.FindRemovableOverhang -> Color.SCARLET
            Phase.PlaceTileOnTarget -> when (subPhase) {
                SubPhase.ExploreColumn -> Color.BLUE
                SubPhase.ReturnToColumnTop -> Color.TEAL
                SubPhase.ExploreBoundary -> Color.SKY
                SubPhase.ReturnToBoundary -> Color.YELLOW
                else -> throw Exception("Incompatible phases: Phase $phase with sub-phase $subPhase")
            }
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
            val moveLabel = overhangNbrLabel()!!
            moveToLabel(moveLabel)
            entryTile = true
            outerLabel = (moveLabel + 3).mod(6)
            phase = Phase.FindRemovableOverhang
            subPhase = SubPhase.SearchAndLiftOverhang
            return
        }

        traverseTargetTileBoundary()
    }

    private fun searchAndLiftOverhang() {
        if (!hasOverhangNbr() || ((!entryTile || !hasTargetTileNbr()) && isOnTile() && isAtOverhangEdge())) {
            liftTile()
            subPhase = SubPhase.LeaveOverhang
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
            subPhase = SubPhase.CompressOverhang
            return
        }

        if (hasTargetTileNbr()) {
            entryTile = false
        }

        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

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
        moveToLabel((compressDir + 2).mod(6))
        outerLabel = (compressDir + 4).mod(6)
        entryTile = true
        subPhase = SubPhase.SearchAndLiftOverhang
    }

    private fun leaveOverhang() {
        if (hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            columnDir = moveLabel
            phase = Phase.PlaceTileOnTarget
            subPhase = SubPhase.ExploreColumn
            return
        }
        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Enter phase: [Phase.PlaceTileOnTarget]:[SubPhase.ExploreColumn]
     *
     * The robot moves along a column (direction specified by [columnDir]) of target nodes until it either reaches the
     * column's end (a non-target node) or an empty target node where it can place the tile it is carrying.
     *
     * Exit phases:
     *   [Phase.PlaceTileOnTarget]:[SubPhase.ReturnToBoundary]
     *   [Phase.PlaceTileOnTarget]:[SubPhase.ReturnToColumnTop]
     */
    private fun exploreColumn() {
        // Place tile if empty target node on column
        if (carriesTile && !isOnTile()) {
            placeTile()
            subPhase = SubPhase.ReturnToBoundary
            return
        }

        // Move along column as long as possible, then turn around
        if (labelIsTarget(columnDir)) {
            moveAndUpdate(columnDir)
        } else {
            subPhase = SubPhase.ReturnToColumnTop
        }
    }

    /**
     * Enter phase: [Phase.PlaceTileOnTarget]:[SubPhase.ReturnToColumnTop]
     *
     * The robot moves back up the column (opposite direction of [columnDir]) until it reaches the column's end.
     * Then it moves to the next tile along the boundary (unless the target tile structure only consists of one column).
     *
     * Exit phases:
     *   [Phase.PlaceTileOnTarget]:[SubPhase.ExploreBoundary]
     *   [Phase.PlaceTileOnTarget]:[SubPhase.ExploreColumn]
     */
    private fun returnToColumnTop() {
        val revColumnDir = (columnDir + 3).mod(6)
        if (hasTileAtLabel(revColumnDir) && labelIsTarget(revColumnDir)) {
            moveAndUpdate(revColumnDir)
            return
        }

        outerLabel = revColumnDir
        (1..6).map { (outerLabel + it).mod(6) }.forEach { label ->
            if (labelIsTarget(label)) {
                moveAndUpdate(label)
                subPhase = SubPhase.ExploreBoundary
                return
            }
        }

        subPhase = SubPhase.ExploreColumn
    }

    /**
     * Enter phase: [Phase.PlaceTileOnTarget]:[SubPhase.ExploreBoundary]
     *
     * The robot moves along the boundary of the target shape and triggers column searches or places its tile when
     * possible.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.PlaceTileOnTarget]:[SubPhase.ExploreColumn]
     */
    private fun exploreBoundary() {
        if (!isOnTile()) {
            placeTile()
            phase = Phase.FindOverhang
            return
        }

        if ((enterAngle in 0..2
                && !labelIsTarget((columnDir + 3).mod(6))
                && (enterAngle == 2 || !labelIsTarget((columnDir + 2).mod(6))))
            || ((enterAngle == 4 || enterAngle == 5) && (0..3).all { offset ->
                !labelIsTarget((columnDir + offset).mod(6))
            })
        ) {
            subPhase = SubPhase.ExploreColumn
            return
        }
        var label = (enterAngle + columnDir).mod(6)
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

    /**
     * Enter phase: [Phase.PlaceTileOnTarget]:[SubPhase.ReturnToBoundary]
     *
     * Exit phase: [Phase.FindOverhang]
     */
    private fun returnToBoundary() {
        val revColumnDir = (columnDir + 3).mod(6)
        if ((hasTileAtLabel(revColumnDir) && !labelIsTarget(revColumnDir)) || !hasTileAtLabel(revColumnDir)) {
            outerLabel = revColumnDir
            phase = Phase.FindOverhang
            return
        }
        moveToLabel(revColumnDir)
    }

    /**
     * Helper function
     *
     * Moves the robot to the given [label] and updates the [enterAngle] (label of entry relative to [columnDir]).
     * Only used in [Phase.PlaceTileOnTarget].
     */
    private fun moveAndUpdate(label: Int) {
        moveToLabel(label)
        enterAngle = (label + 3 - columnDir).mod(6)
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target tile, it instead
     * traverses the boundary of the entire tile structure until it encounters a target tile.
     */
    private fun traverseTargetTileBoundary() {
        // Set label of the outer boundary if the variable is not set yet.
        // This requires the robot to be positioned at a node where its only adjacent boundary is the outer boundary.
        if (outerLabel < 0) {
            outerLabel = if (hasOverhangNbr()) {
                overhangNbrLabel()!!
            } else if (hasEmptyNonTargetNbr()) {
                emptyNonTargetNbrLabel()!!
            } else {
                throw Exception("Robot at node $node is not at target tile boundary.")
            }
        }

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
     * Checks whether the robot is at an edge of a connected component of overhang tiles where a tile can safely be
     * removed.
     */
    private fun isAtOverhangEdge(): Boolean {
        val boundaryLabels = labels.filter { label -> labelIsTarget(label) || !hasTileAtLabel(label) }

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
