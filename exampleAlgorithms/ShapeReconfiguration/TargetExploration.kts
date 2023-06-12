/**
 * Modification of arbitrary layer traversal algorithm from https://ris.uni-paderborn.de/record/25126
 * Based on maze exploration algorithm from https://doi.org/10.1109/SFCS.1978.30
 * Modification: Using O(log n) height counter instead of pebbles for simplicity
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
    LiftLastPebble,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = true,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.TraverseColumn
    private var prevPhase = Phase.ReturnSouth

    private var delta = 0
    private var height = 0
    private var enterLabel = 0

    private var numPebbleTiles = 2
    private var pebbleMoveDir = 0

    private var hasMoved = false
    private var hasResult = false
    private var result = false

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
            Phase.LiftLastPebble -> liftLastPebble()
        }
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.TraverseColumn -> Color.ORANGE
            Phase.ReturnSouth -> Color.TEAL
            Phase.TraverseBoundary -> Color.BROWN
            Phase.DetectUniquePoint, Phase.LiftLastPebble -> Color.PINK
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
            prevPhase = Phase.TraverseColumn
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
            prevPhase = Phase.TraverseBoundary
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
        val moveLabel = (1..6).map { (enterLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        moveAndUpdate(moveLabel)
    }

    private fun detectUniquePoint() {
        if (isOnTarget() && numPebbleTiles == 2 && (!labelIsTarget(5) || !labelIsTarget(1))) {
            hasResult = true
            result = false
            phase = prevPhase
            return
        }

        if (numPebbleTiles == 2) {
            if (isOnTarget()) {
                pebbleMoveDir = labels.first { label -> !labelIsTarget(label) }
                moveToLabel(pebbleMoveDir)
                return
            }
            placeTile()
            numPebbleTiles--
            return
        }

        if (!isOnTarget()) {
            moveToLabel((pebbleMoveDir + 3).mod(6))
            return
        }

        if (!carriesTile) {
            liftTile()
            return
        }

        moveAndUpdate(5)
        delta = 0
        height = 1
        phase = Phase.FindFirstCandidate
    }

    private fun findFirstCandidate() {
        if (height <= 0) {
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
        val moveLabel = (1..6).map { (enterLabel - it).mod(6) }.first { label -> labelIsTarget(label) }
        moveAndUpdate(moveLabel)
    }

    private fun findLoop() {
        if (height <= 0 && hasMoved) {
            hasMoved = false
            if (enterLabel != 1 || height < 0) {
                phase = Phase.Backtrack
            } else if (enterLabel == 1 && delta == 1) {
                result = true
                phase = Phase.ReturnWithResult
            }
            return
        }

        // Move around the boundary by RHR
        val moveLabel = (1..6).map { (enterLabel - it).mod(6) }.first { label -> labelIsTarget(label) }
        moveAndUpdate(moveLabel)
    }

    private fun backtrack() {
        if (height == 0) {
            delta = 0
            phase = Phase.BacktrackFurther
            return
        }

        if (!hasMoved) {  // Move back once before switching to LHR
            moveAndUpdate(enterLabel)
            return
        }

        // Move around the boundary by LHR
        val moveLabel = (1..6).map { (enterLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        moveAndUpdate(moveLabel)
    }

    private fun backtrackFurther() {
        if (!hasMoved) {  // Move back once before switching to LHR
            moveAndUpdate(enterLabel)
            return
        }

        if (height <= 0 && enterLabel == 5 && delta == 0) {
            result = false
            hasResult = true
            phase = Phase.LiftLastPebble
            return
        }

        // Move around the boundary by LHR
        val moveLabel = (1..6).map { (enterLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        moveAndUpdate(moveLabel)
    }

    private fun returnWithResult() {
        if (!hasMoved) {  // Move back once before switching to LHR
            moveAndUpdate(enterLabel)
            return
        }

        if (height <= 0) {
            hasResult = true
            phase = Phase.LiftLastPebble
            return
        }

        // Move around the boundary by LHR
        val moveLabel = (1..6).map { (enterLabel + it).mod(6) }.first { label -> labelIsTarget(label) }
        moveAndUpdate(moveLabel)
    }

    private fun liftLastPebble() {
        if (numPebbleTiles < 2) {
            if (carriesTile) {
                placeTile()
                return
            }
            if (isOnTarget()) {
                pebbleMoveDir = labels.first { label -> hasTileAtLabel(label) && !labelIsTarget(label) }
                moveToLabel(pebbleMoveDir)
                return
            }
            liftTile()
            numPebbleTiles++
            return
        }

        moveToLabel((pebbleMoveDir + 3).mod(6))
        phase = prevPhase
        return
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
        height += when (label) {
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
