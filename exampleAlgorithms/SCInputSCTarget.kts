fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindBoundary, FindTargetOnBoundary, FindOverhang, FindOverhangTile, FindEmptyTarget
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0
) {
    private var phase = Phase.FindBoundary

    private var searchDir: Int? = null
    private var moveLabel: Int = 0

    override fun activate() {
        when (phase) {
            Phase.FindBoundary -> findBoundary()
            Phase.FindTargetOnBoundary -> findTargetOnBoundary()
            Phase.FindOverhang -> findOverhang()
            Phase.FindOverhangTile -> findOverhangTile()
            Phase.FindEmptyTarget -> findEmptyTarget()
        }
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindBoundary -> Color.BLUE
            Phase.FindTargetOnBoundary -> Color.TEAL
            Phase.FindOverhang -> Color.ORANGE
            Phase.FindOverhangTile -> Color.SCARLET
            Phase.FindEmptyTarget -> Color.SKY
        }
    }

    private fun findBoundary() {
        if (labels.any { label -> !hasTileAtLabel(label) }) {  // Check for empty node neighbour
            phase = Phase.FindTargetOnBoundary
            return
        }
        moveToLabel(0)
    }

    private fun findTargetOnBoundary() {
        if (isOnTarget()) {
            phase = Phase.FindOverhang
            return
        }
        boundaryTraversal(true)
    }

    private fun findOverhang() {
        if (!isOnTarget()) {
            phase = Phase.FindOverhangTile
            return
        }

        boundaryTraversal(true)
    }

    private fun findOverhangTile() {
        if (numBoundaries() == 1) {
            liftTile()
            phase = Phase.FindEmptyTarget
            return
        }

        boundaryTraversal(true, true)
    }

    private fun findEmptyTarget() {
        if (isOnTarget() && numBoundaries(true) == 1) {
            placeTile()
            phase = Phase.FindOverhang
            return
        }

        boundaryTraversal(false, isOnTarget())
    }

    private fun boundaryTraversal(inside: Boolean, onlySameNodeType: Boolean = false) {
        val canMoveTo = if (inside && onlySameNodeType) {
            { label: Int -> hasTileAtLabel(label) && !labelIsTarget(label) }
        } else if (inside) {
            { label: Int -> hasTileAtLabel(label) }
        } else if (onlySameNodeType) {
            { label: Int -> !hasTileAtLabel(label) && labelIsTarget(label) }
        } else {
            { label: Int -> !hasTileAtLabel(label) }
        }

        val invalidLabel = (1..6).map { (moveLabel - it).mod(6) }.first { label -> !canMoveTo(label) }
        moveLabel = (1..5).map { (invalidLabel + it).mod(6) }.first { label -> canMoveTo(label) }
        moveToLabel(moveLabel)
    }
}
