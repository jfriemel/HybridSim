/**
 * Exploration algorithm for target tile shape with holes without using pebbles
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
    PlaceEmulatedPebble,
    LiftEmulatedPebble,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = true,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.TraverseColumn
    private var preDetectionPhase = Phase.ReturnSouth
    private var prePebblePhase = Phase.DetectUniquePoint

    private var delta = 0  // Acceptable values: {0, 1, 2, 3, 4}
    private var heightSign = 0  // Acceptable values: {-1, 0, 1}
    private var enterLabel = 0  // Acceptable values: {0, 1, 2, 3, 4, 5}

    private var numEmulatedPebbles = 2  // Acceptable values: {0, 1, 2}
    private var pebbleMoveDir = 0  // Acceptable values: {0, 1, 2, 3, 4, 5}
    private var pebbleStep = 0  // Acceptable values (depends on context): {0, 1, 2, 3, 4}
    private var pebbleNbr = -1  // Acceptable values: {-1, 0, 1, 2, 3, 4, 5}

    private var withTile = carriesTile
    private var hasMoved = false
    private var hasResult = false
    private var result = false
    private var prevLhr = true

    override fun activate() {
        if (phase in arrayOf(Phase.TraverseColumn, Phase.ReturnSouth, Phase.TraverseBoundary)) {
            // A visual indicator for the user to see which tiles have been visited
            // Tiles are not colored when the robot is in the unique point detection subroutine
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
            Phase.PlaceEmulatedPebble -> placeEmulatedPebble()
            Phase.LiftEmulatedPebble -> liftEmulatedPebble()
        }
        println("$numEmulatedPebbles  $pebbleStep  $pebbleNbr")
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.TraverseColumn -> Color.ORANGE
            Phase.ReturnSouth -> Color.TEAL
            Phase.TraverseBoundary -> Color.BROWN
            Phase.DetectUniquePoint, Phase.PlaceEmulatedPebble, Phase.LiftEmulatedPebble -> Color.PINK
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
            pebbleStep = 0
            prePebblePhase = Phase.DetectUniquePoint
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
        moveBoundary(lhr = false)
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
        moveBoundary(lhr = false)
    }

    private fun backtrack() {
        if (heightSign == 0) {
            delta = 0
            phase = Phase.BacktrackFurther
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true)
    }

    private fun backtrackFurther() {
        if (hasMoved && heightSign <= 0 && enterLabel == 5 && delta == 0) {
            result = false
            hasResult = true
            pebbleStep = 0
            prePebblePhase = preDetectionPhase
            phase = Phase.LiftEmulatedPebble
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true)
    }

    private fun returnWithResult() {
        if (hasMoved && heightSign <= 0) {
            hasResult = true
            pebbleStep = 0
            prePebblePhase = preDetectionPhase
            phase = Phase.LiftEmulatedPebble
            return
        }

        // Move around the boundary by LHR
        moveBoundary(lhr = true)
    }

    private fun placeEmulatedPebble() {
        when (pebbleStep) {
            0 -> {
                val pebbleNbrCandidate = labels.filter {
                        label -> labelIsTarget(label) && !hasTileAtLabel(label)
                }.firstOrNull()
                if (pebbleNbrCandidate != null) {
                    pebbleNbr = pebbleNbrCandidate
                    numEmulatedPebbles--
                    phase = prePebblePhase
                } else if (withTile) {
                    pebbleMoveDir = labels.first { label -> !labelIsTarget(label) }
                    moveToLabel(pebbleMoveDir)
                } else {
                    liftTile()
                }
            }

            1 -> {
                if (withTile) {
                    placeTile()
                } else {
                    pebbleMoveDir = labels.first { label -> !labelIsTarget(label) }
                    moveToLabel(pebbleMoveDir)
                }
            }

            2 -> {
                if (withTile) {
                    moveToLabel((pebbleMoveDir + 3).mod(6))
                } else {
                    placeTile()
                }
            }

            3 -> {
                if (withTile) {
                    liftTile()
                } else {
                    moveToLabel((pebbleMoveDir + 3).mod(6))
                }
                numEmulatedPebbles--
                phase = prePebblePhase
            }

            else -> throw Exception("Step $pebbleStep invalid in phase $phase")
        }

        pebbleStep++
    }

    private fun liftEmulatedPebble() {
        when (pebbleStep) {
            0 -> {
                if (isOnTile() && pebbleNbr >= 0 && labelIsTarget(pebbleNbr) && !hasTileAtLabel(pebbleNbr)) {
                    pebbleNbr = -1
                    numEmulatedPebbles++
                    phase = prePebblePhase
                } else if (withTile) {
                    placeTile()
                } else {
                    pebbleMoveDir = labels.first { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
                    moveToLabel(pebbleMoveDir)
                }
            }

            1 -> {
                if (withTile) {
                    pebbleMoveDir = labels.first { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
                    moveToLabel(pebbleMoveDir)
                } else {
                    liftTile()
                }
            }

            2 -> {
                if (withTile) {
                    liftTile()
                } else {
                    moveToLabel((pebbleMoveDir + 3).mod(6))
                }
            }

            3 -> {
                if (withTile) {
                    moveToLabel((pebbleMoveDir + 3).mod(6))
                } else {
                    placeTile()
                }
                if (pebbleNbr < 0) {
                    numEmulatedPebbles++
                    phase = prePebblePhase
                }
            }

            4 -> {
                moveToLabel((pebbleNbr + 3).mod(6))
                pebbleStep = -1
                phase = Phase.PlaceEmulatedPebble
            }

            else -> throw Exception("Step $pebbleStep invalid in phase $phase")
        }

        pebbleStep++
    }

    private fun moveBoundary(lhr: Boolean) {
        val moveLabel = if (lhr && prevLhr) {
            (1..6).map { (enterLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        } else if (!lhr && !prevLhr) {
            (1..6).map { (enterLabel - it).mod(6) }.first { label -> labelIsTarget(label) }
        } else {
            enterLabel
        }
        moveAndUpdate(moveLabel)
        prevLhr = lhr
    }

    private fun moveAndUpdate(label: Int) {
        val turnDegree = (label - (enterLabel + 3)).mod(6)
        // Update turn-delta
        if (phase in arrayOf(Phase.FindFirstCandidate, Phase.FindLoop)) {  // RHR traversal
            if (turnDegree in 3..5) {
                delta = (delta - (6 - turnDegree)).mod(5)
            } else if (turnDegree == 1) {
                delta = (delta + 1).mod(5)
            }
        } else {  // LHR traversal
            if (turnDegree in 1..3) {
                delta = (delta - turnDegree).mod(5)
            } else if (turnDegree == 5) {
                delta = (delta + 1).mod(5)
            }
        }

        // Update height
        heightSign += when (label) {
            0 -> 2  // north
            1, 5 -> 1  // north-east, north-west
            2, 4 -> -1  // south-east, south-west
            3 -> -2  // south
            else -> 0  // impossible direction
        }

        // Move and update enter label
        moveToLabel(label)
        enterLabel = (label + 3).mod(6)
        hasMoved = true
    }
}
