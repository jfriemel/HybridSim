fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, 0)
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
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.FindBoundary

    private var entryTile: Boolean = false
    private var outerLabel: Int? = null
    private var moveDir: Int? = null

    override fun activate() {
//        print("$node  $moveDir -- ")
        when (phase) {
            Phase.FindBoundary -> findBoundary()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.FindOverhang -> findOverhang()
            Phase.FindRemovableOverhang -> findRemovableOverhang()
            Phase.FindDemandComponent -> findDemandComponent()
            Phase.PlaceTargetTile -> placeTargetTile()
        }
//        println("$node  $moveDir")
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
     * Exit phase: [Phase.FindOverhang]
     */
    private fun findBoundary() {
        if (!isAtBoundary()) {
            move()
            return
        }

        outerLabel = labels.first { label -> !hasTileAtLabel(label) }

        if (isOnTarget()) {
            phase = Phase.FindOverhang
            return
        } else if (hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            if (moveToLabel(moveLabel)) {
                outerLabel = (moveLabel + 3).mod(6)
                phase = Phase.FindOverhang
                return
            }
        }
        phase = Phase.LeaveOverhang
    }

    /**
     * Enter phase: [Phase.LeaveOverhang]
     *
     * The robot traverses the boundary of the overhang component until it reaches a target tile.
     *
     * Exit phases:
     *   [Phase.FindDemandComponent]
     *   [Phase.FindOverhang]
     */
    private fun leaveOverhang() {
        if (hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            if (moveToLabel(moveLabel)) {
                outerLabel = (moveLabel + 3).mod(6)
                phase = if (carriesTile) {
                    Phase.FindDemandComponent
                } else {
                    Phase.FindOverhang
                }
            }
        } else {
            move()
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
            if (moveToLabel(moveLabel)) {
                outerLabel = (moveLabel + 3).mod(6)
                entryTile = true
                phase = Phase.FindRemovableOverhang
                return
            }
        }

        move()
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
     * Exit phases:
     *   [Phase.FindDemandComponent]
     *   [Phase.LeaveOverhang]
     */
    private fun findRemovableOverhang() {
        if (((!entryTile && isAtOverhangBorder()) || !hasOverhangNbr())) {
            if (hasHangingRobotNbr()) {
                phase = Phase.LeaveOverhang
            } else {
                liftTile()
                phase = Phase.FindDemandComponent
            }
            return
        }

        move()
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
            if (moveToLabel(moveLabel)) {
                outerLabel = (moveLabel + 3).mod(6)
                phase = Phase.PlaceTargetTile
                return
            }
        } else if (!isOnTarget() && hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            if (moveToLabel(moveLabel)) {
                outerLabel = (moveLabel + 3).mod(6)
                return
            }
        }

        move()
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

        move()
    }

    /**
     * Helper function
     *
     * The robot moves based on its current [phase].
     */
    private fun move() {
        updateMoveDir()

        if (moveDir != null && moveToLabel(moveDir!!)) {
            outerLabel = (moveDir!! + 3).mod(6)
            entryTile = false
        } else if (
            moveDir != null &&
            hasRobotAtLabel(moveDir!!) &&
            (robotAtLabel(moveDir!!) as RobotImpl).moveDir == (moveDir!! + 3).mod(6)
        ) {
            switch(moveDir!!)
            entryTile = false
        } else {
            run findCopyableNbr@{
                allRobotNbrLabels().map { label ->
                    Pair<Int, RobotImpl>(label, robotAtLabel(label)!! as RobotImpl)
                }.forEach { (label, robotNbr) ->
                    robotNbr.updateMoveDir()
                    if (robotNbr.moveDir != (label + 3).mod(6)) {
                        return@forEach
                    }
                    val prevPhase = phase
                    val prevOuterLabel = outerLabel
                    val prevMoveDir = moveDir
                    phase = robotNbr.phase
                    outerLabel = label
                    updateMoveDir()
                    if (moveDir != null && moveDir != prevMoveDir) {
                        val prevCarriesTile = carriesTile
                        carriesTile = robotNbr.carriesTile
                        robotNbr.carriesTile = prevCarriesTile
                        entryTile = false
                        // Move one step
                        return@findCopyableNbr
                    }
                    phase = prevPhase
                    outerLabel = prevOuterLabel
                    moveDir = prevMoveDir
                }
            }
        }

        updateMoveDir()
    }

    /**
     * Helper function
     *
     * The robot switches positions with the robot at label [switchLabel].
     */
    private fun switch(switchLabel: Int) {
        val robotNbr = robotAtLabel(switchLabel)!! as RobotImpl
        switchWithRobotNbr(switchLabel)
        outerLabel = (switchLabel - 2).mod(6)
        robotNbr.outerLabel = (switchLabel + 1).mod(6)
        robotNbr.updateMoveDir()
        updateMoveDir()
    }

    /**
     * Helper function
     *
     * Updates the [moveDir] pointer based on the robot's current [phase].
     */
    private fun updateMoveDir() {
        when (phase) {
            Phase.FindBoundary -> {
                moveDir = if (hasTileAtLabel(0)) 0 else null
            }

            Phase.LeaveOverhang -> {
                moveDir = getBoundaryMoveLabel { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
            }

            Phase.FindOverhang -> {
                moveDir = getBoundaryMoveLabel { label -> hasTileAtLabel(label) && labelIsTarget(label) }
            }

            Phase.FindRemovableOverhang -> {
                moveDir = getBoundaryMoveLabel { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
            }

            Phase.FindDemandComponent -> {
                moveDir = getBoundaryMoveLabel { label ->
                    hasTileAtLabel(label) && labelIsTarget(label) == isOnTarget()
                }
            }

            Phase.PlaceTargetTile -> {
                moveDir = if (outerLabel != null) {
                    (1..6).map { offset -> (outerLabel!! - offset).mod(6) }
                        .firstOrNull { label -> !hasTileAtLabel(label) }
                } else {
                    null
                }
            }
        }
    }

    /**
     * Helper function
     *
     * @return The label for the next movement direction in a traversal of the boundary of nodes whose labels are
     * considered valid by [isLabelValid].
     */
    private fun getBoundaryMoveLabel(isLabelValid: (Int) -> Boolean): Int? {
        return if (outerLabel != null) {
            (1..6).map { offset -> (outerLabel!! + offset).mod(6) }.firstOrNull(isLabelValid)
        } else {
            null
        }
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
