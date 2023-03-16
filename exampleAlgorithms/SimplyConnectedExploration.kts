fun getRobot(orientation: Int, node: Node): Robot {
    return RobotImpl(orientation, node)
}

enum class Phase {
    TC, RS, TB
}

class RobotImpl(orientation: Int, node: Node): Robot(
    orientation = orientation,
    node = node,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0
) {
    private var phase: Phase = Phase.TC
    private var enterLabel: Int = 0

    override fun getColor(): Color {
        return when (phase) {
            Phase.TC -> Color.BLUE
            Phase.RS -> Color.ORANGE
            Phase.TB -> Color.MAGENTA
        }
    }

    override fun activate() {
        tileBelow()?.setColor(Color.GREEN)
        when (phase) {
            Phase.TC -> traverseColumn()
            Phase.RS -> returnSouth()
            Phase.TB -> traverseBoundary()
        }
    }

    private fun traverseColumn() {
        if (hasTileAtLabel(0)) {
            moveAndUpdate(0)
        } else {
            phase = Phase.RS
        }
    }

    private fun returnSouth() {
        if (hasTileAtLabel(3)) {
            moveAndUpdate(3)
            return
        }
        for (label in arrayOf(4, 5, 0, 1, 2)) {
            if (hasTileAtLabel(label)) {
                moveAndUpdate(label)
                phase = Phase.TB
                return
            }
        }
        phase = Phase.TC
    }

    private fun traverseBoundary() {
        if ((0 <= enterLabel && enterLabel <= 2 && !hasTileAtLabel(3) && (enterLabel == 2 || !hasTileAtLabel(2)))
                || ((enterLabel == 4 || enterLabel == 5) && !hasTileAtLabel(0) && !hasTileAtLabel(1) && !hasTileAtLabel(2) && !hasTileAtLabel(3))) {
            phase = Phase.TC
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
