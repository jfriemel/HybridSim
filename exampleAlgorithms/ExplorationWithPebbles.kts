/**
 * Arbitrary layer traversal algorithm from https://ris.uni-paderborn.de/record/25126
 * Based on maze exploration algorithm from https://doi.org/10.1109/SFCS.1978.30
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
    ReturnWithResult,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 2,
    maxPebbles = 2,
) {
    private var phase = Phase.TraverseColumn
    private var prevPhase = Phase.ReturnSouth

    private var delta = 0
    private var result = false

    private var enterLabel = 0

    override fun activate() {
        tileBelow()?.setColor(Color.SKY)  // Only a visual indicator for the user to see which tiles have been visited
        when (phase) {
            Phase.TraverseColumn -> traverseColumn()
            Phase.ReturnSouth -> returnSouth()
            Phase.TraverseBoundary -> traverseBoundary()
            else -> return
        }
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.TraverseColumn -> Color.ORANGE
            Phase.ReturnSouth -> Color.TEAL
            Phase.TraverseBoundary -> Color.BROWN
            else -> Color.SCARLET
        }
    }

    private fun traverseColumn() {
        if (hasTileAtLabel(0)) {
            moveAndUpdate(0)
        } else {
            phase = Phase.ReturnSouth
        }
    }

    private fun returnSouth() {
        if (hasTileAtLabel(3)) {
            moveAndUpdate(3)
            return
        }

        intArrayOf(4, 5, 0, 1, 2).firstOrNull { label -> hasTileAtLabel(label) }?.let { label ->
            moveAndUpdate(label)
            phase = Phase.TraverseBoundary
            return
        }

        phase = Phase.TraverseColumn
    }

    private fun traverseBoundary() {
        if ((enterLabel in 0..2 && !hasTileAtLabel(3) && (enterLabel == 2 || !hasTileAtLabel(2)))
            || ((enterLabel == 4 || enterLabel == 5) && intArrayOf(0, 1, 2, 3).all { !hasTileAtLabel(it) })
        ) {
            phase = Phase.TraverseColumn
            return
        }

        val moveLabel = (1..6).map { (enterLabel + it).mod(6) }.first { label -> hasTileAtLabel(label) }
        moveAndUpdate(moveLabel)
    }

    private fun detectUniquePoint() {
        if (!hasTileAtLabel(5) || !hasTileAtLabel(1)) {
            phase = prevPhase
            return
        }

        delta = 0
        if (!tileHasPebble()) {
            putPebble()
            return
        }
        phase = Phase.FindFirstCandidate
    }

    private fun findFirstCandidate() {

    }

    private fun moveAndUpdate(label: Int) {
        moveToLabel(label)
        enterLabel = (label + 3).mod(6)
    }
}
