fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, 0)
}

private enum class Phase {
    FindBoundary,
    LeaveOverhang,
    FindOverhang,
    FindRemovableOverhang,
    Hanging,
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
    private var hasCheckedTile: Boolean = false
    private var outerLabel: Int? = null
    private var moveDir: Int? = null

    override fun activate() {
        print("$node  $moveDir  $outerLabel -- ")
        when (phase) {
            Phase.FindBoundary -> findBoundary()
            Phase.LeaveOverhang -> leaveOverhang()
            Phase.FindOverhang -> findOverhang()
            Phase.FindRemovableOverhang -> findRemovableOverhang()
            Phase.Hanging -> hanging()
            Phase.FindDemandComponent -> findDemandComponent()
            Phase.PlaceTargetTile -> placeTargetTile()
        }
        println("$node  $moveDir  $outerLabel")
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindBoundary -> Color.TEAL
            Phase.LeaveOverhang -> Color.YELLOW
            Phase.FindOverhang -> Color.ORANGE
            Phase.FindRemovableOverhang -> Color.SCARLET
            Phase.Hanging -> Color.BROWN
            Phase.FindDemandComponent -> Color.SKY
            Phase.PlaceTargetTile -> Color.WHITE
        }
    }

    /** The robot walks north until it reaches the boundary of the input shape. */
    private fun findBoundary() {
        if (!isAtBoundary()) {
            move()
            return
        }

        outerLabel = labels.first { label -> !hasTileAtLabel(label) }

        if (isOnTarget()) {
            phase = if (carriesTile) Phase.FindDemandComponent else Phase.FindOverhang
            return
        } else if (hasTargetTileNbr()) {
            val moveLabel = targetTileNbrLabel()!!
            if (moveToLabel(moveLabel)) {
                outerLabel = (moveLabel + 3).mod(6)
                phase = if (carriesTile) Phase.FindDemandComponent else Phase.FindOverhang
                return
            }
        }
        phase = Phase.LeaveOverhang
    }

    /** The robot traverses the boundary of the overhang component until it reaches a target tile. */
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
     * The robot traverses the target tile boundary until it encounters an overhang tile.
     * It moves to the tile and sets [entryTile] to true.
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
     * The robot traverses the overhang boundary until it encounters a tile that can be removed without breaking the
     * connectivity of the overhang component (checked by [isAtOverhangBorder]).
     * To make sure that the robot does not disconnect the overhang component from the rest of the tile structure, it
     * does not lift the first overhang tile it moved to (checked by [entryTile]) unless it is the only tile of the
     * component.
     */
    private fun findRemovableOverhang() {
        if (((!entryTile && isAtOverhangBorder()) || !hasOverhangNbr())) {
            if (hasHangingRobotNbr()) {
                phase = Phase.LeaveOverhang
            } else {
                liftTile()
                phase = Phase.Hanging
            }
            return
        }

        hasCheckedTile = true
        move()
    }

    /**
     * The robot traverses the outside of the tile boundary until it can enter the tile shape without being blocked by
     * another robot.
     */
    private fun hanging() {
        if (carriesTile) {
            val robotNbr = labels.firstOrNull { label ->
                hasRobotAtLabel(label) && robotAtLabel(label)!!.isOnTile() && !robotAtLabel(label)!!.carriesTile
            }?.let { label -> robotAtLabel(label)!! as RobotImpl }
            if (robotNbr != null) {
                carriesTile = false
                robotNbr.carriesTile = true
                robotNbr.phase = if (robotNbr.isOnTarget()) {
                    Phase.FindDemandComponent
                } else {
                    Phase.LeaveOverhang
                }
            }
        }

        val nextLabel = labels.firstOrNull { label -> hasTileAtLabel(label) && !hasRobotAtLabel(label) }
        if (nextLabel != null) {
            moveToLabel(nextLabel)
            outerLabel = (nextLabel + 3).mod(6)
            phase = if (isOnTarget() && carriesTile) {
                Phase.FindDemandComponent
            } else if (isOnTarget() && !carriesTile) {
                Phase.FindOverhang
            } else {
                Phase.LeaveOverhang
            }
            return
        } else {
            move()
        }
    }

    /** The robot traverses the target tile boundary until it can move to demand node. */
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
     * The robot traverses the boundary of the connected component of demand nodes until it reaches a border where it
     * places the tile it is carrying without creating a hole in the intermediate target shape.
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
     * The robot moves (or passes its carried tile) based on its current [phase].
     */
    private fun move() {
        updateMoveDir()

        if (moveDir != null && moveToLabel(moveDir!!)) {
            outerLabel = if (phase == Phase.PlaceTargetTile) (moveDir!! + 2).mod(6) else (moveDir!! - 2).mod(6)
            entryTile = false
            hasCheckedTile = false
        } else if (
                moveDir != null &&
                hasRobotAtLabel(moveDir!!) &&
                (robotAtLabel(moveDir!!) as RobotImpl).moveDir == (moveDir!! + 3).mod(6)
        ) {
            switch(moveDir!!)
        } else if (
                moveDir != null &&
                carriesTile &&
                isOnTile() &&
                hasRobotAtLabel(moveDir!!) &&
                !robotAtLabel(moveDir!!)!!.carriesTile
        ) {
            val robotNbr = (robotAtLabel(moveDir!!)!! as RobotImpl)
            carriesTile = false
            phase = if (isOnTarget()) {
                Phase.FindOverhang
            } else {
                Phase.LeaveOverhang
            }
            robotNbr.carriesTile = true
            robotNbr.outerLabel = (moveDir!! + 1).mod(6)
            robotNbr.phase = if (robotNbr.isOnTarget()) {
                Phase.FindDemandComponent
            } else {
                Phase.LeaveOverhang
            }
        } else {
            run findCopyableNbr@{
                val prevPhase = phase
                val prevOuterLabel = outerLabel
                val prevMoveDir = moveDir
                val prevHasCheckedTile = hasCheckedTile
                allRobotNbrLabels().map { label ->
                    Pair<Int, RobotImpl>(label, robotAtLabel(label)!! as RobotImpl)
                }.forEach { (label, robotNbr) ->
                    robotNbr.updateMoveDir()
                    if (robotNbr.moveDir != (label + 3).mod(6)) {
                        return@forEach
                    }
                    if (!carriesTile) {
                        phase = robotNbr.phase
                    }
                    outerLabel = if (phase == Phase.Hanging || phase == Phase.PlaceTargetTile) {  // RHR
                        (label - 1).mod(6)
                    } else {  // LHR
                        (label + 1).mod(6)
                    }
                    hasCheckedTile = true
                    updateMoveDir()
                    if (moveDir != null && moveDir != prevMoveDir) {
                        if (robotNbr.carriesTile && !carriesTile) {
                            robotNbr.carriesTile = false
                            carriesTile = true
                            robotNbr.phase = if (robotNbr.isOnTarget()) {
                                Phase.FindOverhang
                            } else if (robotNbr.isOnTile()) {
                                Phase.LeaveOverhang
                            } else {
                                Phase.Hanging
                            }
                        }
                        entryTile = false
                        hasCheckedTile = false
                        // TODO: Move one step
                        return@findCopyableNbr
                    }
                }
                phase = prevPhase
                outerLabel = prevOuterLabel
                moveDir = prevMoveDir
                hasCheckedTile = prevHasCheckedTile
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

        outerLabel = if (phase == Phase.Hanging || phase == Phase.PlaceTargetTile) {  // RHR
            (switchLabel + 2).mod(6)
        } else {  // LHR
            (switchLabel - 2).mod(6)
        }
        robotNbr.outerLabel = if (robotNbr.phase == Phase.Hanging || robotNbr.phase == Phase.PlaceTargetTile) {  // RHR
            (switchLabel - 1).mod(6)
        } else {  // LHR
            (switchLabel + 1).mod(6)
        }
        entryTile = false
        hasCheckedTile = false
        robotNbr.entryTile = false
        robotNbr.hasCheckedTile = false

        updateMoveDir()
        robotNbr.updateMoveDir()
    }

    /**
     * Helper function
     *
     * Updates the [moveDir] pointer based on the robot's current [phase].
     */
    private fun updateMoveDir() {
        if (outerLabel == null || (getValidLabelFunction())(outerLabel!!)) {
            outerLabel = labels.firstOrNull { label -> !(getValidLabelFunction())(label) }
            if (outerLabel == null) {
                phase = Phase.FindBoundary
            }
        }

        when (phase) {
            Phase.FindBoundary -> {
                moveDir = if (hasTileAtLabel(0)) 0 else null
            }

            Phase.LeaveOverhang, Phase.FindOverhang, Phase.FindDemandComponent -> {
                moveDir = getBoundaryMoveLabel(getValidLabelFunction())
            }

            Phase.FindRemovableOverhang -> {
                moveDir = if (hasCheckedTile) {
                    getBoundaryMoveLabel(getValidLabelFunction())
                } else {
                    null
                }
            }

            Phase.PlaceTargetTile, Phase.Hanging -> {
                moveDir = (1..6).map { offset -> (outerLabel!! - offset).mod(6) }.firstOrNull(getValidLabelFunction())
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

    private fun getValidLabelFunction(): (Int) -> Boolean {
        return when (phase) {
            Phase.FindBoundary ->
                { label -> label == 0 }
            Phase.LeaveOverhang, Phase.FindRemovableOverhang ->
                { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
            Phase.FindOverhang, Phase.FindDemandComponent ->
                { label -> hasTileAtLabel(label) && labelIsTarget(label) }
            Phase.PlaceTargetTile, Phase.Hanging ->
                { label -> !hasTileAtLabel(label) }
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
