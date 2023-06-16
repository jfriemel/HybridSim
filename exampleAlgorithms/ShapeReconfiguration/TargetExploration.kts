/**
 * Exploration algorithm for (some) target tile shapes with holes without using pebbles
 * Partially based on arbitrary layer traversal algorithm from https://ris.uni-paderborn.de/record/25126
 * Based on maze exploration algorithm from https://doi.org/10.1109/SFCS.1978.30
 * Pebbles are simulated by moving tiles from target nodes to non-target nodes at boundaries.
 */

fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    TraverseColumn,
    ReturnSouth,
    TraverseBoundary,
    DetectUniquePoint,
    FindFirstCandidate,
    FindLoop,
    Backtrack,
    BacktrackFurther,
    ReturnWithResult,
    MoveEmulatedPebbles,
    PlaceEmulatedPebble,
    LiftEmulatedPebble,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = true,  // Robot may carry initial tile, but this is not required
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.TraverseColumn  // Current phase
    private var preDetectionPhase = Phase.ReturnSouth  // Phase before initiating unique point detection
    private var prePebbleMovePhase = Phase.FindFirstCandidate  // Phase before moving height pebbles
    private var prePebbleTogglePhase = Phase.DetectUniquePoint  // Phase before lifting/placing a single pebble

    // Keep track of turn-directions mod 5 for tracking horizontal displacement
    private var delta = 0  // Acceptable values: {0, 1, 2, 3, 4}
    // Keep track of the sign (+1, 0, -1) of the current height (using emulated pebbles)
    private var heightSign = 0  // Acceptable values: {-1, 0, 1}
    // Keep track of the direction from which the robot entered its current node
    private var enterLabel = 0  // Acceptable values: {0, 1, 2, 3, 4, 5}

    // Reset enterLabel to previous value before height update subroutine
    private var preEnterLabel = 0  // Acceptable values: {0, 1, 2, 3, 4, 5}

    // Keep track of how many pebbles the robot is currently carrying
    private var numEmulatedPebbles = 2  // Acceptable values: {0, 1, 2}
    // If the robot wants to place a pebble next to another pebble, it emulates the new pebble by saving a direction
    // marker its pebble neighbor
    private var pebbleNbr = -1  // Acceptable values: {-1, 0, 1, 2, 3, 4, 5}
    // If the robot wants to place an overhang tile to emulate a pebble, but the boundary is already blocked by a
    // different pebble, the robot moves the new overhang tile over the existing overhang tile and remembers the
    // movement direction
    private var pebbleOverhangNbr = -1  // Acceptable values: {-1, 0, 1, 2, 3, 4, 5}
    // Last movement direction when placing/lifting a pebble; used to ensure that the robot returns to its original node
    private var pebbleToggleDir = 0  // Acceptable values: {0, 1, 2, 3, 4, 5}
    // Distance by which the robot needs to move the B pebble to keep the correct distance between the pebbles
    private var pebbleMoveDist = 0  // Acceptable values: {-1, 0, 1, 2, 3}

    // Pebble currently being placed/lifted; 0 for A pebble, 1 for B pebble
    private var currentPebble = 0  // Acceptable values: {0, 1}
    // Direction of the overhang tile placed for emulating pebble A
    private var pebbleADir = -1  // Acceptable values: {-1, 0, 1, 2, 3, 4, 5}
    // Direction of the overhang tile placed for emulating pebble B
    private var pebbleBDir = -1  // Acceptable values: {-1, 0, 1, 2, 3, 4, 5}

    private var pebbleMoveStep = 0  // Acceptable values depend on context, but only finitely many possible
    private var pebbleToggleStep = 0  // Acceptable values depend on context, but only finitely many possible

    private var withTile = carriesTile
    private var hasMoved = false
    private var hasResult = false
    private var result = false
    private var prevLhr = true

    override fun activate() {
        if (isOnTarget() && !isOnEmulatedPebble()) {
            // A visual indicator for the user to see which tiles have been visited
            tileBelow()?.setColor(Color.SKY)
        }
        when (phase) {
            Phase.TraverseColumn -> traverseColumn()
            Phase.ReturnSouth -> returnSouth()
            Phase.TraverseBoundary -> traverseBoundary()
            Phase.DetectUniquePoint -> detectUniquePoint()
            Phase.FindFirstCandidate -> findFirstCandidate()
            Phase.FindLoop -> findLoop()
            Phase.Backtrack -> backtrack()
            Phase.BacktrackFurther -> backtrackFurther()
            Phase.ReturnWithResult -> returnWithResult()
            Phase.MoveEmulatedPebbles -> moveEmulatedPebbles()
            Phase.PlaceEmulatedPebble -> placeEmulatedPebble()
            Phase.LiftEmulatedPebble -> liftEmulatedPebble()
        }
        println("p=$phase  d=$pebbleMoveDist  m=$pebbleMoveStep  t=$pebbleToggleStep  h=$heightSign  n=$numEmulatedPebbles  e=$enterLabel  del=$delta  o=$pebbleOverhangNbr  dirs=[$pebbleADir,$pebbleBDir]")
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.TraverseColumn -> Color.ORANGE
            Phase.ReturnSouth -> Color.TEAL
            Phase.TraverseBoundary -> Color.BROWN
            Phase.DetectUniquePoint, Phase.PlaceEmulatedPebble, Phase.LiftEmulatedPebble, Phase.MoveEmulatedPebbles
            -> Color.PINK
            Phase.FindFirstCandidate -> Color.SCARLET
            Phase.FindLoop -> Color.YELLOW
            Phase.Backtrack, Phase.BacktrackFurther -> Color.BLUE
            Phase.ReturnWithResult -> if (result) Color.WHITE else Color.GRAY
        }
    }

    private fun traverseColumn() {
        if (hasResult) {
            hasResult = false
            if (result) {
                moveAndUpdate(1)
                phase = Phase.TraverseBoundary
            } else {
                phase = Phase.ReturnSouth
            }
            return
        }

        if (labelIsTarget(0)) {
            moveAndUpdate(0)
        }  else {
            preDetectionPhase = Phase.TraverseColumn
            phase = Phase.DetectUniquePoint
        }
    }

    private fun returnSouth() {
        if (labelIsTarget(3)) {
            moveAndUpdate(3)
            return
        }

        // Move one step
        intArrayOf(4, 5, 0, 1, 2).firstOrNull { label -> labelIsTarget(label) }?.let { label ->
            moveAndUpdate(label)
            phase = Phase.TraverseBoundary
            return
        }

        phase = Phase.TraverseColumn
    }

    private fun traverseBoundary() {
        if (hasResult) {
            hasResult = false
            if (result) {
                phase = Phase.ReturnSouth
            } else {
                hasMoved = false
            }
            return
        }

        if (enterLabel == 5 && hasMoved) {
            preDetectionPhase = Phase.TraverseBoundary
            phase = Phase.DetectUniquePoint
            return
        }

        if (
            (enterLabel in 0..2 && !labelIsTarget(3) && (enterLabel == 2 || !labelIsTarget(2)))
            || ((enterLabel == 4 || enterLabel == 5) && intArrayOf(0, 1, 2, 3).all { !labelIsTarget(it) })
        ) {
            phase = Phase.TraverseColumn
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true)
    }

    private fun detectUniquePoint() {
        if (isOnTarget() && numEmulatedPebbles == 2 && (!labelIsTarget(5) || !labelIsTarget(1))) {
            hasResult = true
            result = false
            phase = preDetectionPhase
            return
        }

        if (numEmulatedPebbles == 2) {
            pebbleToggleStep = 0
            prePebbleTogglePhase = Phase.DetectUniquePoint
            currentPebble = 1
            phase = Phase.PlaceEmulatedPebble
            return
        }

        moveAndUpdate(5)
        prevLhr = false
        delta = 0
        heightSign = 1
        phase = Phase.FindFirstCandidate
    }

    private fun findFirstCandidate() {
        if (heightSign <= 0) {
            if (enterLabel == 1 && delta == 0) {
                hasMoved = false
                phase = Phase.FindLoop
            } else {
                result = false
                hasMoved = false
                phase = Phase.ReturnWithResult
            }
            return
        }

        // Move around the boundary by RHR
        moveBoundary(lhr = false, updateHeight = true, currentPhase = Phase.FindFirstCandidate)
    }

    private fun findLoop() {
        if (hasMoved && heightSign <= 0) {
            hasMoved = false
            if (enterLabel != 1 || heightSign < 0) {
                phase = Phase.Backtrack
            } else if (enterLabel == 1 && delta == 1) {
                result = true
                phase = Phase.ReturnWithResult
            }
            return
        }

        // Move around the boundary by RHR
        moveBoundary(lhr = false, updateHeight = true, currentPhase = Phase.FindLoop)
    }

    private fun backtrack() {
        if (heightSign == 0) {
            delta = 0
            phase = Phase.BacktrackFurther
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true, updateHeight = true, currentPhase = Phase.Backtrack)
    }

    private fun backtrackFurther() {
        if (hasMoved && heightSign == 0 && enterLabel == 5 && delta == 0) {
            result = false
            hasResult = true
            pebbleToggleStep = 0
            prePebbleTogglePhase = preDetectionPhase
            phase = Phase.LiftEmulatedPebble
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true, updateHeight = true, currentPhase = Phase.BacktrackFurther)
    }

    private fun returnWithResult() {
        if (hasMoved && heightSign == 0) {
            hasResult = true
            pebbleToggleStep = 0
            prePebbleTogglePhase = preDetectionPhase
            phase = Phase.LiftEmulatedPebble
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true, updateHeight = true, currentPhase = Phase.ReturnWithResult)
    }

    private fun moveEmulatedPebbles() {
        val rhr = prePebbleMovePhase in arrayOf(Phase.FindFirstCandidate, Phase.FindLoop)

        when (pebbleMoveStep) {
            0 -> {
                when ((enterLabel + 3).mod(6)) {  // determine how far pebble needs to be moved forwards
                    0 -> pebbleMoveDist = -1
                    1, 5 -> pebbleMoveDist = 0
                    2, 4 -> pebbleMoveDist = 2
                    3 -> pebbleMoveDist = 3
                }

                if (isOnEmulatedPebble()) {
                    pebbleMoveStep = 3
                    pebbleMoveDist = - pebbleMoveDist
                    pebbleADir = pebbleBDir
                    if (enterLabel in intArrayOf(1, 2, 4, 5)) {
                        heightSign = 0
                    }
                    return
                }
                if (heightSign == 0) {
                    heightSign = 1
                }

                if (pebbleMoveDist == 0) {
                    enterLabel = preEnterLabel
                    phase = prePebbleMovePhase
                } else {
                    pebbleToggleStep = 0
                    prePebbleTogglePhase = Phase.MoveEmulatedPebbles
                    currentPebble = 0
                    phase = Phase.PlaceEmulatedPebble
                    pebbleMoveStep++
                }
            }

            1 -> {
                moveBoundary(lhr = rhr)
                if (isOnEmulatedPebble()) {
                    pebbleMoveStep++
                }
            }

            2 -> {
                pebbleToggleStep = 0
                prePebbleTogglePhase = Phase.MoveEmulatedPebbles
                currentPebble = 1
                phase = Phase.LiftEmulatedPebble
                pebbleMoveStep++
            }

            3 -> {
                if (isOnEmulatedPebble()) {
                    heightSign = - heightSign
                    pebbleMoveDist = - pebbleMoveDist
                }
                if (pebbleMoveDist < 0) {
                    moveBoundary(lhr = rhr)
                    pebbleMoveDist++
                } else if (pebbleMoveDist > 0) {
                    moveBoundary(lhr = !rhr)
                    pebbleMoveDist--
                }
                if (pebbleMoveDist == 0) {
                    pebbleMoveStep++
                }
            }

            4 -> {
                if (isOnEmulatedPebble()) {
                    heightSign = 0
                    pebbleBDir = pebbleADir
                    phase = prePebbleMovePhase
                } else {
                    pebbleToggleStep = 0
                    prePebbleTogglePhase = Phase.MoveEmulatedPebbles
                    currentPebble = 1
                    phase = Phase.PlaceEmulatedPebble
                }
                pebbleMoveStep++
            }

            5 -> {
                moveBoundary(lhr = !rhr)
                pebbleMoveStep++
            }

            6 -> {
                if (isOnEmulatedPebble()) {
                    pebbleToggleStep = 0
                    enterLabel = preEnterLabel
                    prePebbleTogglePhase = prePebbleMovePhase
                    currentPebble = 0
                    phase = Phase.LiftEmulatedPebble
                } else {
                    moveBoundary(lhr = !rhr)
                }
            }
        }
    }

    private fun placeEmulatedPebble() {
        when (pebbleToggleStep) {
            0 -> {
                val pebbleNbrCandidate = labels.firstOrNull { label ->
                    labelIsTarget(label) && !hasTileAtLabel(label) && canMoveToLabel(label)
                }
                if (pebbleNbrCandidate != null) {
                    pebbleNbr = pebbleNbrCandidate
                    numEmulatedPebbles--
                    phase = prePebbleTogglePhase
                    tileBelow()?.setColor(Color.PURPLE)
                } else if (withTile) {
                    val tmp = labels.firstOrNull { label ->
                        !labelIsTarget(label) && !hasTileAtLabel(label)
                            && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                    }
                    if (tmp == null) {
                        pebbleToggleDir = labels.first { label ->
                            !labelIsTarget(label)
                                && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                        }
                        pebbleToggleStep = 19
                    } else {
                        pebbleToggleDir = tmp!!
                    }
                    if (currentPebble == 0) {
                        pebbleADir = pebbleToggleDir
                    } else {
                        pebbleBDir = pebbleToggleDir
                    }
                    moveToLabel(pebbleToggleDir)
                } else {
                    liftTile()
                }
            }

            1 -> {
                if (withTile) {
                    if (labels.filter { label -> !labelIsTarget(label) }.isEmpty()) {
                        moveToLabel((pebbleToggleDir + 3).mod(6))
                        result = true
                        hasResult = true
                        phase = preDetectionPhase
                    } else {
                        placeTile()
                    }
                } else {
                    val tmp = labels.firstOrNull { label ->
                        !labelIsTarget(label) && !hasTileAtLabel(label)
                            && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                    }
                    if (tmp == null) {
                        pebbleToggleDir = labels.first { label ->
                            !labelIsTarget(label)
                                && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                        }
                        pebbleToggleStep = 19
                    } else {
                        pebbleToggleDir = tmp!!
                    }
                    if (currentPebble == 0) {
                        pebbleADir = pebbleToggleDir
                    } else {
                        pebbleBDir = pebbleToggleDir
                    }
                    moveToLabel(pebbleToggleDir)
                }
            }

            2 -> {
                if (withTile) {
                    moveToLabel((pebbleToggleDir + 3).mod(6))
                } else {
                    if (labels.filter { label -> !labelIsTarget(label) }.isEmpty()) {
                        moveToLabel((pebbleToggleDir + 3).mod(6))
                        pebbleToggleStep = 9
                    } else {
                        placeTile()
                    }
                }
            }

            3 -> {
                if (withTile) {
                    liftTile()
                } else {
                    moveToLabel((pebbleToggleDir + 3).mod(6))
                }
                numEmulatedPebbles--
                phase = prePebbleTogglePhase
            }

            10 -> {
                placeTile()
                result = true
                hasResult = true
                phase = preDetectionPhase
            }

            20 -> {
                pebbleOverhangNbr = labels.first { label ->
                    !labelIsTarget(label) && !hasTileAtLabel(label)
                        && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                }
                moveToLabel(pebbleOverhangNbr)
            }

            21 -> {
                placeTile()
            }

            22 -> {
                moveToLabel((pebbleOverhangNbr + 3).mod(6))
                pebbleToggleStep = if (withTile) 1 else 2
            }

            else -> throw Exception("Step $pebbleToggleStep invalid in phase $phase")
        }

        pebbleToggleStep++
    }

    private fun liftEmulatedPebble() {
        when (pebbleToggleStep) {
            0 -> {
                if (isOnTile() && pebbleNbr >= 0 && labelIsTarget(pebbleNbr) && !hasTileAtLabel(pebbleNbr)) {
                    pebbleNbr = -1
                    numEmulatedPebbles++
                    phase = prePebbleTogglePhase
                } else if (withTile) {
                    placeTile()
                } else {
                    pebbleToggleDir = if (currentPebble == 0) pebbleADir else pebbleBDir
                    moveToLabel(pebbleToggleDir)
                }
            }

            1 -> {
                if (withTile) {
                    pebbleToggleDir = if (currentPebble == 0) pebbleADir else pebbleBDir
                    moveToLabel(pebbleToggleDir)
                } else {
                    if (
                        pebbleOverhangNbr >= 0 && !labelIsTarget(pebbleOverhangNbr) && hasTileAtLabel(pebbleOverhangNbr)
                    ) {
                        moveToLabel(pebbleOverhangNbr)
                        pebbleToggleStep = 9
                    } else {
                        liftTile()
                    }
                }
            }

            2 -> {
                if (withTile) {
                    if (
                        pebbleOverhangNbr >= 0 && !labelIsTarget(pebbleOverhangNbr) && hasTileAtLabel(pebbleOverhangNbr)
                    ) {
                        moveToLabel(pebbleOverhangNbr)
                        pebbleToggleStep = 9
                    } else {
                        liftTile()
                    }
                } else {
                    moveToLabel((pebbleToggleDir + 3).mod(6))
                }
            }

            3 -> {
                if (withTile) {
                    moveToLabel((pebbleToggleDir + 3).mod(6))
                } else {
                    placeTile()
                }
                if (pebbleNbr < 0) {
                    numEmulatedPebbles++
                    phase = prePebbleTogglePhase
                }
            }

            4 -> {
                moveToLabel((pebbleNbr + 3).mod(6))
            }

            5 -> {
                if (withTile) {
                    pebbleToggleDir = labels.first { label ->
                        !labelIsTarget(label) && !hasTileAtLabel(label)
                            && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                    }
                    if (currentPebble == 0) {
                        pebbleBDir = pebbleToggleDir
                    } else {
                        pebbleADir = pebbleToggleDir
                    }
                    moveToLabel(pebbleToggleDir)
                } else {
                    liftTile()
                }
            }

            6 -> {
                if (withTile) {
                    placeTile()
                } else {
                    pebbleToggleDir = labels.first { label ->
                        !labelIsTarget(label) && !hasTileAtLabel(label)
                            && (labelIsTarget((label - 1).mod(6)) || labelIsTarget((label + 1).mod(6)))
                    }
                    if (currentPebble == 0) {
                        pebbleBDir = pebbleToggleDir
                    } else {
                        pebbleADir = pebbleToggleDir
                    }
                    moveToLabel(pebbleToggleDir)
                }
            }

            7 -> {
                if (withTile) {
                    moveToLabel((pebbleToggleDir + 3).mod(6))
                } else {
                    placeTile()
                }
            }

            8 -> {
                if (withTile) {
                    liftTile()
                } else {
                    moveToLabel((pebbleToggleDir + 3).mod(6))
                }
            }

            9 -> {
                moveToLabel(pebbleNbr)
                pebbleNbr = -1
                numEmulatedPebbles++
                phase = prePebbleTogglePhase
            }

            10 -> {
                liftTile()
            }

            11 -> {
                moveToLabel(((pebbleOverhangNbr) + 3).mod(6))
                pebbleOverhangNbr = -1
                pebbleToggleStep = if (withTile) 2 else 1
            }

            else -> throw Exception("Step $pebbleToggleStep invalid in phase $phase")
        }

        pebbleToggleStep++
    }

    private fun isOnEmulatedPebble(): Boolean =
        !isOnTile() || (pebbleNbr >= 0 && labelIsTarget(pebbleNbr) && !hasTileAtLabel(pebbleNbr))

    private fun moveBoundary(lhr: Boolean, updateHeight: Boolean = false, currentPhase: Phase = Phase.FindLoop) {
        val moveLabel = if (lhr && prevLhr) {
            (1..6).map { (enterLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        } else if (!lhr && !prevLhr) {
            (1..6).map { (enterLabel - it).mod(6) }.first { label -> labelIsTarget(label) }
        } else {
            enterLabel
        }
        moveAndUpdate(moveLabel, updateDelta = updateHeight)
        prevLhr = lhr

        if (updateHeight) {
            preEnterLabel = enterLabel
            prePebbleMovePhase = currentPhase
            pebbleMoveStep = 0
            phase = Phase.MoveEmulatedPebbles
        }
    }

    private fun moveAndUpdate(label: Int, updateDelta: Boolean = true) {
        val turnDegree = (label - (enterLabel + 3)).mod(6)
        // Update turn-delta
        if (phase in arrayOf(Phase.FindFirstCandidate, Phase.FindLoop)) {  // RHR traversal
            if (updateDelta) {
                if (turnDegree in 3..5) {
                    delta = (delta - (6 - turnDegree)).mod(5)
                } else if (turnDegree == 1) {
                    delta = (delta + 1).mod(5)
                }
            }
        } else {  // LHR traversal
            if (updateDelta) {
                if (turnDegree in 1..3) {
                    delta = (delta - turnDegree).mod(5)
                } else if (turnDegree == 5) {
                    delta = (delta + 1).mod(5)
                }
            }
        }

        // Move and update enter label
        moveToLabel(label)
        enterLabel = (label + 3).mod(6)
        hasMoved = true
    }
}
