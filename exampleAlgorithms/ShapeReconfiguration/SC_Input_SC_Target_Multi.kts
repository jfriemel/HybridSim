fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node)
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

class RobotImpl(node: Node) :
    Robot(
        node = node,
        orientation = 0, // common chirality
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
        when (phase) {
            // No extra functionality needed on top of move() with updatePhase() and updateMoveDir()
            Phase.FindBoundary, Phase.LeaveOverhang, Phase.FindOverhang, Phase.FindDemandComponent,
            -> move()

            Phase.FindRemovableOverhang -> findRemovableOverhang()
            Phase.Hanging -> hanging()
            Phase.PlaceTargetTile -> placeTargetTile()
        }
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

    /**
     * The robot traverses the outside of the tile boundary until it can enter the tile shape
     * without being blocked by another robot.
     */
    private fun hanging() {
        if (carriesTile) {
            val robotNbr =
                labels
                    .firstOrNull { label ->
                        hasRobotAtLabel(label) &&
                            robotAtLabel(label)!!.isOnTile() &&
                            !robotAtLabel(label)!!.carriesTile
                    }
                    ?.let { label -> robotAtLabel(label)!! as RobotImpl }
            if (robotNbr != null) {
                carriesTile = false
                robotNbr.carriesTile = true
                robotNbr.phase =
                    if (robotNbr.isOnTarget()) {
                        Phase.FindDemandComponent
                    } else {
                        Phase.LeaveOverhang
                    }
            }
        }

        val nextLabel =
            labels.firstOrNull { label -> hasTileAtLabel(label) && !hasRobotAtLabel(label) }
        if (nextLabel != null) {
            moveToLabel(nextLabel)
            outerLabel = (nextLabel + 3).mod(6)
            phase =
                if (isOnTarget() && carriesTile) {
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

    /**
     * The robot traverses its overhang component until it finds a safely removable tile or a
     * hanging robot neighbor which carries a tile.
     */
    private fun findRemovableOverhang() {
        if ((!entryTile && isAtOverhangBorder()) || !hasOverhangNbr()) {
            if (
                !hasHangingRobotNbr() ||
                allHangingRobotNbrLabels().all { label ->
                    hasTileAtLabel((label - 1).mod(6)) || hasTileAtLabel((label + 1).mod(6))
                }
            ) {
                liftTile()
                phase = Phase.Hanging
            } else {
                allHangingRobotNbrs()
                    .firstOrNull { robotNbr ->
                        robotNbr.carriesTile && (robotNbr as RobotImpl).phase == Phase.Hanging
                    }
                    ?.let { robotNbr ->
                        robotNbr.carriesTile = false
                        carriesTile = true
                        phase = Phase.LeaveOverhang
                        return
                    }
            }
        } else {
            move()
        }
    }

    /**
     * The robot traverses the boundary of the connected component of demand nodes until it reaches
     * a border where it places the tile it is carrying without creating a hole in the intermediate
     * target shape.
     */
    private fun placeTargetTile() {
        if (isAtDemandBorder()) {
            placeTile()
            outerLabel = labels.first { label -> !(hasTileAtLabel(label) && labelIsTarget(label)) }
            phase = Phase.FindOverhang
        } else {
            move()
        }
    }

    /**
     * Helper function
     *
     * The robot moves (or passes its carried tile) based on its current [phase].
     */
    private fun move() {
        updatePhase()
        hasCheckedTile = true
        updateMoveDir()

        if (moveDir != null && moveToLabel(moveDir!!)) {
            outerLabel = (moveDir!! - 2).mod(6)
            entryTile = false
            hasCheckedTile = false
        } else if (
            moveDir != null &&
            hasRobotAtLabel(moveDir!!) &&
            (
                (robotAtLabel(moveDir!!) as RobotImpl).moveDir == (moveDir!! + 3).mod(6) ||
                    (robotAtLabel(moveDir!!) as RobotImpl).phase == Phase.FindBoundary
                )
        ) {
            if (
                phase == Phase.FindRemovableOverhang &&
                (robotAtLabel(moveDir!!) as RobotImpl).phase == Phase.FindRemovableOverhang
            ) {
                phase = Phase.LeaveOverhang
            }
            switch(moveDir!!)
        } else if (
            moveDir != null &&
            carriesTile &&
            hasRobotAtLabel(moveDir!!) &&
            !robotAtLabel(moveDir!!)!!.carriesTile
        ) {
            val robotNbr = (robotAtLabel(moveDir!!)!! as RobotImpl)
            carriesTile = false
            robotNbr.carriesTile = true
            robotNbr.outerLabel = (moveDir!! - 2).mod(6)
            robotNbr.phase =
                if (phase == Phase.PlaceTargetTile || phase == Phase.Hanging) {
                    phase
                } else if (labelIsTarget(moveDir!!)) {
                    Phase.FindDemandComponent
                } else {
                    Phase.LeaveOverhang
                }
            phase =
                if (!isOnTile()) {
                    Phase.Hanging
                } else if (isOnTarget()) {
                    Phase.FindOverhang
                } else {
                    Phase.LeaveOverhang
                }
            updatePhase()
            updateMoveDir()
            robotNbr.updatePhase()
            robotNbr.updateMoveDir()
        } else if (
            moveDir != null &&
            (phase == Phase.FindRemovableOverhang || phase == Phase.FindOverhang) &&
            hasRobotAtLabel(moveDir!!) &&
            (robotAtLabel(moveDir!!)!! as RobotImpl).phase == Phase.LeaveOverhang &&
            !robotAtLabel(moveDir!!)!!.carriesTile
        ) {
            val robotNbr = (robotAtLabel(moveDir!!)!! as RobotImpl)
            robotNbr.outerLabel = (moveDir!! - 2).mod(6)
            robotNbr.entryTile = isOnTarget()
            robotNbr.hasCheckedTile = false
            robotNbr.phase = Phase.FindRemovableOverhang
            robotNbr.updateMoveDir()
            phase =
                if (isOnTarget()) {
                    Phase.FindOverhang
                } else {
                    Phase.LeaveOverhang
                }
        } else {
            run findCopyableNbr@{
                val prevPhase = phase
                val prevOuterLabel = outerLabel
                val prevMoveDir = moveDir
                allRobotNbrLabels()
                    .map { label ->
                        Pair<Int, RobotImpl>(label, robotAtLabel(label)!! as RobotImpl)
                    }
                    .forEach { (label, robotNbr) ->
                        robotNbr.updateMoveDir()
                        if (robotNbr.moveDir != (label + 3).mod(6)) {
                            return@forEach
                        }
                        if (!carriesTile) {
                            phase =
                                if (robotNbr.phase == Phase.FindRemovableOverhang) {
                                    Phase.LeaveOverhang
                                } else {
                                    robotNbr.phase
                                }
                        }
                        outerLabel = (label + 1).mod(6)
                        updatePhase()
                        updateMoveDir()
                        if (moveDir != null && moveDir != prevMoveDir) {
                            if (robotNbr.carriesTile && !carriesTile) {
                                robotNbr.carriesTile = false
                                carriesTile = true
                                robotNbr.phase =
                                    if (robotNbr.isOnTarget()) {
                                        Phase.FindOverhang
                                    } else if (robotNbr.isOnTile()) {
                                        Phase.LeaveOverhang
                                    } else {
                                        Phase.Hanging
                                    }
                            }
                            return@findCopyableNbr
                        }
                    }
                phase = prevPhase
                outerLabel = prevOuterLabel
                moveDir = prevMoveDir
            }
        }

        updatePhase()
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
        entryTile = false
        hasCheckedTile = false
        robotNbr.entryTile = false
        robotNbr.hasCheckedTile = false

        robotNbr.updatePhase()
        robotNbr.updateMoveDir()
    }

    /**
     * Helper function
     *
     * Updates the robot's [phase] based on its current node. Can be called by other robots because
     * it only uses information visible by all neighbors.
     */
    private fun updatePhase() {
        when (phase) {
            Phase.FindBoundary -> {
                if (!isOnTile()) {
                    phase = Phase.Hanging
                }
            }

            Phase.LeaveOverhang -> {
                if (isOnTarget()) {
                    phase =
                        if (carriesTile) {
                            Phase.FindDemandComponent
                        } else {
                            Phase.FindOverhang
                        }
                }
            }

            Phase.FindOverhang -> {
                if (!isOnTarget()) {
                    entryTile = true
                    phase = Phase.FindRemovableOverhang
                }
            }

            Phase.FindRemovableOverhang -> {
                if (isOnTarget()) {
                    phase = Phase.FindOverhang
                }
            }

            Phase.FindDemandComponent -> {
                if (!isOnTile() && isOnTarget()) {
                    phase = Phase.PlaceTargetTile
                }
            }

            else -> return
        }
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
                moveDir = if (isOnTile()) 0 else null
            }

            Phase.LeaveOverhang,
            Phase.FindOverhang,
            Phase.FindDemandComponent,
            Phase.PlaceTargetTile,
            Phase.Hanging,
            -> {
                moveDir = getBoundaryMoveLabel(getValidLabelFunction())
            }

            Phase.FindRemovableOverhang -> {
                moveDir =
                    if (hasCheckedTile) {
                        getBoundaryMoveLabel(getValidLabelFunction())
                    } else {
                        null
                    }
            }
        }
    }

    /**
     * Helper function
     *
     * @return The label for the next movement direction in a traversal of the boundary of nodes
     *   whose labels are considered valid by [isLabelValid].
     */
    private fun getBoundaryMoveLabel(isLabelValid: (Int) -> Boolean): Int? {
        return if (outerLabel != null) {
            (1..6).map { offset -> (outerLabel!! + offset).mod(6) }.firstOrNull(isLabelValid)
        } else {
            return null
        }
    }

    /**
     * Helper function
     *
     * @return A function that checks for a given label whether the robot can move there in its
     *   current [phase].
     */
    private fun getValidLabelFunction(): (Int) -> Boolean {
        return when (phase) {
            Phase.FindBoundary -> { label -> label == 0 }
            Phase.FindOverhang,
            Phase.LeaveOverhang,
            -> { label -> hasTileAtLabel(label) }

            Phase.FindRemovableOverhang -> { label ->
                hasTileAtLabel(label) && !labelIsTarget(label)
            }

            Phase.FindDemandComponent -> { label -> labelIsTarget(label) }
            Phase.PlaceTargetTile,
            Phase.Hanging,
            -> { label -> !hasTileAtLabel(label) }
        }
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of an overhang component where a tile can safely be
     * removed.
     */
    private fun isAtOverhangBorder(): Boolean = isAtBorder { label ->
        labelIsTarget(label) || !hasTileAtLabel(label)
    }

    /**
     * Helper function
     *
     * Checks whether the robot is next to the target tile shape and at a border of a connected
     * component of demand nodes where a tile can safely be placed.
     */
    private fun isAtDemandBorder(): Boolean =
        isOnTarget() &&
            !isOnTile() &&
            hasTargetTileNbr() &&
            isAtBorder { label -> !(hasTileAtLabel(label) && labelIsTarget(label)) }

    /**
     * Helper function
     *
     * Checks whether the robot is at a border of a structure whose boundary labels are induced by
     * [isLabelBoundary].
     */
    private fun isAtBorder(isLabelBoundary: (Int) -> Boolean): Boolean {
        val boundaryLabels = labels.filter(isLabelBoundary)

        if (boundaryLabels.size == 6) {
            return true
        }

        return boundaryLabels.filter { label -> (label + 1).mod(6) !in boundaryLabels }.size == 1
    }
}
