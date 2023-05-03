fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    // Hole expansion for finding outer boundary
    FindAnyBoundary,
    TraverseHole,
    ExpandHole,
    MoveTileNorth,

    // Compression and search for removable overhang tile
    FindOverhang,
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
    private var phase = Phase.FindAnyBoundary

    private var entryTile: Boolean = false
    private var outerLabel: Int = -1
    private var enterAngle: Int = 0
    private var columnDir: Int = 0

    private var compressDir: Int = -1
    private var hasMoved: Boolean = false

    override fun activate() {
        when (phase) {
            Phase.FindAnyBoundary -> findAnyBoundary()
            Phase.TraverseHole -> traverseHole()
            Phase.MoveTileNorth -> moveTileNorth()
            Phase.ExpandHole -> expandHole()
            Phase.FindOverhang -> findOverhang()
            Phase.SearchAndLiftOverhang -> searchAndLiftOverhang()
            Phase.CompressOverhang -> compressOverhang()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.ExploreColumn -> exploreColumn()
            Phase.ReturnToColumnTop -> returnToColumnTop()
            Phase.ExploreBoundary -> exploreBoundary()
            Phase.ReturnToBoundary -> returnToBoundary()
        }
    }

    override fun getColor(): Color = when (phase) {
        Phase.FindAnyBoundary, Phase.TraverseHole, Phase.MoveTileNorth, Phase.ExpandHole
            -> Color.ORANGE
        Phase.FindOverhang, Phase.SearchAndLiftOverhang, Phase.CompressOverhang, Phase.LeaveOverhang
            -> Color.SCARLET
        Phase.ExploreColumn, Phase.ReturnToColumnTop, Phase.ExploreBoundary, Phase.ReturnToBoundary
            -> Color.SKY
    }

    /**
     * Enter phase: [Phase.FindAnyBoundary]
     *
     * The robot moves in direction 3 until it either finds the outer boundary or a boundary in direction 3.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.TraverseHole]
     */
    private fun findAnyBoundary() {
        if (outerBoundaryFound()) return

        if (!hasTileAtLabel(3)) {
            outerLabel = 3
            phase = Phase.TraverseHole
            return
        }
        moveToLabel(3)
    }

    /**
     * Enter phase: [Phase.TraverseHole]
     *
     * The robot traverses the boundary and expands it whenever possible (without violating connectivity).
     * When tiles can be safely removed, they are carried in direction 0.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.MoveTileNorth]
     *   [Phase.ExpandHole]
     */
    private fun traverseHole() {
        if (outerBoundaryFound()) return

        if (isAtBorder()) {
            liftTile()
            hasMoved = false
            phase = Phase.MoveTileNorth
            return
        }

        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first { label -> hasTileAtLabel(label) }

        if (
            !hasTileAtLabel((moveLabel + 1).mod(6)) && hasTileAtLabel((moveLabel + 2).mod(6))
            && (hasTileAtLabel((moveLabel + 3).mod(6)) || !hasTileAtLabel((moveLabel + 4).mod(6)))
        ) {
            liftTile()
            compressDir = (moveLabel + 1).mod(6)
            hasMoved = false
            phase = Phase.ExpandHole
            return
        }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    private fun expandHole() {
        if (!hasMoved) {
            moveToLabel(compressDir)
            hasMoved = true
            return
        }
        if (carriesTile) {
            placeTile()
            return
        }
        if (outerBoundaryFound()) return
        moveToLabel((compressDir + 2).mod(6))
        outerLabel = (compressDir + 4).mod(6)
        phase = Phase.TraverseHole
    }

    /**
     * Enter phase: [Phase.MoveTileNorth]
     *
     * The robot moves its carried tile in direction 0 and then returns to its previous boundary by moving in
     * direction 3. If it finds the outer boundary on the way back, it enters the next stage of the algorithm.
     *
     * Exit phases:
     *   [Phase.FindOverhang]
     *   [Phase.TraverseHole]
     */
    private fun moveTileNorth() {
        if (carriesTile) {
            if (!isOnTile() && hasMoved) {
                placeTile()
                return
            }
            if (hasMoved || hasTileAtLabel(0)) {
                moveToLabel(0)
                hasMoved = true
            } else {
                val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.first { label -> hasTileAtLabel(label) }
                moveToLabel(moveLabel)
                outerLabel = (moveLabel - 2).mod(6)
            }
            return
        }
        if (outerBoundaryFound()) return
        if (!hasTileAtLabel(3)) {
            outerLabel = 3
            phase = Phase.TraverseHole
            return
        }
        moveToLabel(3)
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

    private fun searchAndLiftOverhang() {
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

        if (hasTargetTileNbr()) {
            entryTile = false
        }

        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
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
        moveToLabel((compressDir + 2).mod(6))
        outerLabel = (compressDir + 4).mod(6)
        entryTile = true
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
     * Exit phases:
     *   [Phase.ReturnToBoundary]
     *   [Phase.ReturnToColumnTop]
     */
    private fun exploreColumn() {
        // Place tile if demand node on column
        if (carriesTile && !isOnTile()) {
            placeTile()
            phase = Phase.ReturnToBoundary
            return
        }

        // Move along column as long as possible, then turn around
        if (labelIsTarget(columnDir)) {
            moveAndUpdate(columnDir)
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
     *   [Phase.ExploreColumn]
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
                outerLabel = (label - 2).mod(6)
                phase = Phase.ExploreBoundary
                return
            }
        }

        phase = Phase.ExploreColumn
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

        if ((enterAngle in 0..2
                && !labelIsTarget((columnDir + 3).mod(6))
                && (enterAngle == 2 || !labelIsTarget((columnDir + 2).mod(6))))
            || ((enterAngle == 4 || enterAngle == 5) && (0..3).all { offset ->
                !labelIsTarget((columnDir + offset).mod(6))
            })
        ) {
            phase = Phase.ExploreColumn
            return
        }
        (1..6).map { (outerLabel + it).mod(6) }.forEach { label ->
            if (labelIsTarget(label)) {
                moveAndUpdate(label)
                outerLabel = (label - 2).mod(6)
                return
            }
        }
    }

    /**
     * Enter phase: [Phase.ReturnToBoundary]
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

        var numBoundaries = 0
        boundaryLabels.forEach { label ->
            if ((label + 1).mod(6) !in boundaryLabels) {
                numBoundaries++
            }
        }
        return numBoundaries == 1
    }

    private fun outerBoundaryFound(): Boolean {
        if (isOnTarget() && (hasOverhangNbr() || hasEmptyNonTargetNbr())) {
            outerLabel = overhangNbrLabel() ?: emptyNonTargetNbrLabel()!!
            phase = Phase.FindOverhang
            return true
        } else if (!isOnTarget() && hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            phase = Phase.FindOverhang
            return true
        }
        return false
    }
}
