/* Single-robot triangle formation algorithm from https://doi.org/10.1007/s11047-019-09774-2 */

fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    FindBlockTile,
    MoveBlockTile,
    PlaceVertex,
    CarryTileToVertex,
    BuildTriangle,
    BuildNewColumn,
    ReturnToVertex,
    Finished,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.FindBlockTile

    private var testActive = false
    private var numMovementsSW = 0
    private var southernmost = false

    override fun activate() {
        when (phase) {
            Phase.FindBlockTile -> findBlockTile()
            Phase.MoveBlockTile -> moveBlockTile()
            Phase.PlaceVertex -> placeVertex()
            Phase.CarryTileToVertex -> carryTileToVertex()
            Phase.BuildTriangle -> buildTriangle()
            Phase.BuildNewColumn -> buildNewColumn()
            Phase.ReturnToVertex -> returnToVertex()
            Phase.Finished -> return
        }
    }

    override fun finished() = phase == Phase.Finished

    override fun getColor(): Color {
        return when (phase) {
            Phase.FindBlockTile -> Color.ORANGE
            Phase.MoveBlockTile -> Color.TEAL
            Phase.PlaceVertex -> Color.SKY
            Phase.CarryTileToVertex -> Color.BLUE
            Phase.BuildTriangle -> Color.SCARLET
            Phase.BuildNewColumn -> Color.BROWN
            Phase.ReturnToVertex -> Color.YELLOW
            Phase.Finished -> Color.BLACK
        }
    }

    private fun findBlockTile() {
        intArrayOf(5, 4, 0).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                if (label == 4) {
                    numMovementsSW++
                }
                return
            }
        }
        liftTile()
        if (!hasTileAtLabel(1) && !testActive) {
            testActive = true
            southernmost = false
        } else if (!hasTileAtLabel(3)) {
            southernmost = true
        }
        phase = Phase.MoveBlockTile
    }

    private fun moveBlockTile() {
        moveToLabel(2)
        if ((hasTileAtLabel(1) && !hasTileAtLabel(0)) || (hasTileAtLabel(3) && !hasTileAtLabel(4))) {
            testActive = false
        }
        if (!isOnTile()) {
            placeTile()
            if (hasTileAtLabel(2) || numMovementsSW > 1) {
                testActive = false
            } else if (testActive && southernmost) {
                phase = Phase.PlaceVertex
                return
            }
            numMovementsSW = 0
            phase = Phase.FindBlockTile
            return
        }
    }

    private fun placeVertex() {
        if (!carriesTile) {
            liftTile()
            if (!hasTileAtLabel(5)) {
                moveToLabel(0)
            }
            return
        }
        if (hasTileAtLabel(5)) {
            moveToLabel(5)
            return
        }
        moveToLabel(4)
        placeTile()
        phase = Phase.CarryTileToVertex
    }

    private fun carryTileToVertex() {
        if (!carriesTile) {
            if (hasTileAtLabel(1) && !hasTileAtLabel(0)) {
                moveToLabel(1)
                return
            }
            if (!hasTileAtLabel(5) && hasTileAtLabel(0)) {
                moveToLabel(0)
                return
            }
            if (hasTileAtLabel(2)) {
                moveToLabel(2)
                return
            }
            liftTile()
            return
        }

        if (hasTileAtLabel(5)) {
            moveToLabel(5)
            return
        }
        if (hasTileAtLabel(3)) {
            moveToLabel(3)
            return
        }
        moveToLabel(4)
        phase = Phase.BuildTriangle
    }

    private fun buildTriangle() {
        intArrayOf(5, 3).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                return
            }
        }
        if (hasTileAtLabel(2)) {
            moveToLabel(3)
            placeTile()
            phase = Phase.ReturnToVertex
            return
        }
        moveToLabel(5)
        phase = Phase.BuildNewColumn
    }

    private fun buildNewColumn() {
        if (hasTileAtLabel(1)) {
            moveToLabel(0)
            return
        }
        placeTile()
        phase = Phase.ReturnToVertex
    }

    private fun returnToVertex() {
        if (hasTileAtLabel(0) && !hasTileAtLabel(5)) {
            moveToLabel(0)
            return
        }
        if (hasTileAtLabel(2)) {
            moveToLabel(2)
            return
        }
        if (hasTileAtLabel(1)) {
            phase = Phase.CarryTileToVertex
        } else {
            phase = Phase.Finished
        }
    }
}
