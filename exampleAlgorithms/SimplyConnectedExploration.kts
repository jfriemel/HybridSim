/** Compact layer traversal algorithm from https://ris.uni-paderborn.de/record/25126 */

fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    TraverseColumn,
    ReturnSouth,
    TraverseBoundary,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.TraverseColumn

    private var enterLabel = 0

    override fun activate() {
        tileBelow()?.setColor(Color.SKY)
        when (phase) {
            Phase.TraverseColumn -> traverseColumn()
            Phase.ReturnSouth -> returnSouth()
            Phase.TraverseBoundary -> traverseBoundary()
        }
    }

    override fun getColor(): Color {
        return when (phase) {
            Phase.TraverseColumn -> Color.ORANGE
            Phase.ReturnSouth -> Color.TEAL
            Phase.TraverseBoundary -> Color.BROWN
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
        intArrayOf(4, 5, 0, 1, 2).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveAndUpdate(label)
                phase = Phase.TraverseBoundary
                return
            }
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
        var label = enterLabel
        repeat(6) {
            label = (label + 1).mod(6)
            if (hasTileAtLabel(label)) {
                moveAndUpdate(label)
                return
            }
        }
    }

    private fun moveAndUpdate(label: Int) {
        moveToLabel(label)
        enterLabel = (label + 3).mod(6)
    }
}
