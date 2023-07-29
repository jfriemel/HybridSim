fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindBoundary,
    FindOverhang,
    FindRemovableOverhang,
    FindDemandComponent,
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
    private var outerLabel: Int = -1
    private var lhr: Boolean = true

    override fun activate() {
        when (phase) {
            Phase.FindBoundary -> findBoundary()
            Phase.FindOverhang -> findOverhang()
            Phase.FindRemovableOverhang -> findRemovableOverhang()
            Phase.FindDemandComponent -> findDemandComponent()
            Phase.PlaceTargetTile -> placeTargetTile()
        }
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindBoundary -> Color.TEAL
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
     * Then it traverses the boundary until it reaches a target tile.
     *
     * Exit phase: [Phase.FindOverhang]
     */
    private fun findBoundary() {
        if (!isAtBoundary() && outerLabel < 0) {
            moveToLabel(0)
            return
        }

        if (outerLabel < 0) {
            outerLabel = labels.first { label -> !hasTileAtLabel(label) }
        }

        if (isOnTarget()) {
            phase = Phase.FindOverhang
        } else {
            traverseTargetTileBoundary()
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
            outerLabel = (moveLabel + 3).mod(6)
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
     * connectivity of the overhang component (checked by [isAtOverhangBorder]).
     * To make sure that the robot does not disconnect the overhang component from the rest of the tile structure, it
     * does not lift the first overhang tile it moved to (checked by [entryTile]) unless it is the only tile of the
     * component.
     *
     * Exit phase: [Phase.FindDemandComponent]
     */
    private fun findRemovableOverhang() {
        if ((!entryTile && isAtOverhangBorder()) || !hasOverhangNbr()) {
            liftTile()
            phase = Phase.FindDemandComponent
            return
        }

        entryTile = false
        traverseOverhangBoundary()
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
            val moveLabel = emptyTargetNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            lhr = true
            phase = Phase.PlaceTargetTile
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
            outerLabel = labels.first { label -> !(hasTileAtLabel(label) && labelIsTarget(label)) }
            phase = Phase.FindOverhang
            return
        }

        traverseOutsideTileBoundary()
    }

    /**
     * Helper function
     *
     * The traverses the boundary of the target tile structure. If the robot is not on a target tile, it instead
     * traverses the boundary of the entire tile structure until it encounters a target tile.
     */
    private fun traverseTargetTileBoundary() {
        if (!isOnTarget() && hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            moveToLabel(moveLabel)
            outerLabel = (moveLabel + 3).mod(6)
            return
        }

        traverseBoundary { label ->
            canMoveToLabel(label) && hasTileAtLabel(label) && (!isOnTarget() || labelIsTarget(label))
        }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of an overhang component.
     */
    private fun traverseOverhangBoundary() {
        traverseBoundary { label -> canMoveToLabel(label) && hasTileAtLabel(label) && !labelIsTarget(label) }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of a connected component of demand nodes.
     */
    private fun traverseEmptyTargetBoundary() {
        traverseBoundary { label -> canMoveToLabel(label) && !hasTileAtLabel(label) && labelIsTarget(label) }
    }

    /**
     * Helper function
     *
     * The robot traverses the nodes on the outside of the boundary of tiles, provided [outerLabel] points to a tile.
     */
    private fun traverseOutsideTileBoundary() {
        val moveLabelL = (1..6).map { offset -> (outerLabel + offset).mod(6) }.first { label -> !hasTileAtLabel(label) }
        val moveLabelR = (1..6).map { offset -> (outerLabel - offset).mod(6) }.first { label -> !hasTileAtLabel(label) }
        if ((lhr && !labelIsTarget(moveLabelL)) || (!lhr && !labelIsTarget(moveLabelR))) {
            lhr = !lhr
        }
        if (lhr) {
            moveToLabel(moveLabelL)
            outerLabel = (moveLabelL - 2).mod(6)
        } else {
            moveToLabel(moveLabelR)
            outerLabel = (moveLabelR + 2).mod(6)
        }
    }

    /**
     * Helper function
     *
     * The robot traverses the boundary of nodes whose labels are considered valid by [isLabelValid].
     */
    private fun traverseBoundary(isLabelValid: (Int) -> Boolean) {
        val moveLabel = (1..6).map { offset -> (outerLabel + offset).mod(6) }.first(isLabelValid)
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
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
     * Checks whether the robot is next to the target tile shape and at a border of a connected component of demand
     * nodes where a tile can safely be placed.
     */
    private fun isAtDemandBorder(): Boolean =
        isOnTarget() && !isOnTile() && hasTargetTileNbr() && isAtBorder { label ->
            !(hasTileAtLabel(label) && labelIsTarget(label))
        }

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
}
