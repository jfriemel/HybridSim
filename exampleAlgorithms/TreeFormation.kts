/** Single-robot triangle formation algorithm from https://doi.org/10.1007/s11047-019-09774-2 */

fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    Initialize,
    SearchNextBranch,
    SearchNextBranchMoveN,
    CheckOverhangs,
    CheckOverhangsAfterN,
    MoveE,
    HasMovedE,
    GetTileN,
    GetTileNW,
    GetTileNWAfterLift,
    BringTile,
    BringTileMoveS,
    BringTileFinalize,
    Finished,
}

class RobotImpl(node: Node, orientation: Int) : Robot(
    node = node,
    orientation = orientation,
    carriesTile = false,
    numPebbles = 0,
    maxPebbles = 0,
) {
    private var phase = Phase.Initialize

    private var overhangTopFound = false

    override fun activate() {
        when (phase) {
            Phase.Initialize -> initialize()
            Phase.SearchNextBranch -> searchNextBranch()
            Phase.SearchNextBranchMoveN -> searchNextBranchMoveN()
            Phase.CheckOverhangs -> checkOverhangs()
            Phase.CheckOverhangsAfterN -> checkOverhangsAfterN()
            Phase.MoveE -> moveE()
            Phase.HasMovedE -> hasMovedE()
            Phase.GetTileN -> getTileN()
            Phase.GetTileNW -> getTileNW()
            Phase.GetTileNWAfterLift -> getTileNWAfterLift()
            Phase.BringTile -> bringTile()
            Phase.BringTileMoveS -> bringTileMoveS()
            Phase.BringTileFinalize -> bringTileFinalize()
            Phase.Finished -> return
        }
    }

    override fun finished() = phase == Phase.Finished

    override fun getColor(): Color {
        return when (phase) {
            Phase.Initialize -> Color.ORANGE
            Phase.SearchNextBranch, Phase.SearchNextBranchMoveN -> Color.TEAL
            Phase.CheckOverhangs, Phase.CheckOverhangsAfterN -> Color.SKY
            Phase.MoveE, Phase.HasMovedE -> Color.SCARLET
            Phase.GetTileN -> Color.BLUE
            Phase.GetTileNW, Phase.GetTileNWAfterLift -> Color.YELLOW
            Phase.BringTile, Phase.BringTileMoveS, Phase.BringTileFinalize -> Color.BROWN
            Phase.Finished -> Color.BLACK
        }
    }

    private fun initialize() {
        intArrayOf(1, 2, 0).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                return
            }
        }
        phase = Phase.SearchNextBranch
    }

    private fun searchNextBranch() {
        intArrayOf(5, 4).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                phase = Phase.SearchNextBranchMoveN
                return
            }
        }
        if (hasTileAtLabel(3)) {
            moveToLabel(3)
        } else {
            phase = Phase.CheckOverhangs
        }
    }

    private fun searchNextBranchMoveN() {
        if (hasTileAtLabel(0)) {
            moveToLabel(0)
        } else {
            phase = Phase.SearchNextBranch
        }
    }

    private fun checkOverhangs() {
        if (hasTileAtLabel(0)) {
            moveToLabel(0)
        } else {
            overhangTopFound = false
            phase = Phase.CheckOverhangsAfterN
        }
    }

    private fun checkOverhangsAfterN() {
        if (hasTileAtLabel(1) && !hasTileAtLabel(2)) {
            overhangTopFound = true
        } else if (overhangTopFound && !hasTileAtLabel(1) && hasTileAtLabel(2)) {
            phase = Phase.GetTileN
            return
        }
        if (hasTileAtLabel(3)) {
            moveToLabel(3)
        } else {
            phase = Phase.MoveE
        }
    }

    private fun moveE() {
        intArrayOf(2, 1).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                phase = Phase.HasMovedE
                return
            }
        }
        if (hasTileAtLabel(0)) {
            moveToLabel(0)
        } else {
            phase = Phase.Finished
        }
    }

    private fun hasMovedE() {
        phase = if (hasTileAtLabel(3)) {
            moveToLabel(3)
            Phase.SearchNextBranch
        } else {
            Phase.CheckOverhangs
        }
    }

    private fun getTileN() {
        if (hasTileAtLabel(0)) {
            moveToLabel(0)
        } else if (!hasTileAtLabel(5) || hasTileAtLabel(4)) {
            phase = Phase.BringTile
        } else {
            phase = Phase.GetTileNW
        }
    }

    private fun getTileNW() {
        if (carriesTile) {
            moveToLabel(3)
            phase = Phase.GetTileNWAfterLift
        } else if (hasTileAtLabel(5) && !hasTileAtLabel(4) && !hasTileAtLabel(0)) {
            moveToLabel(5)
        } else if (hasTileAtLabel(0)) {
            phase = Phase.GetTileN
        } else if (hasTileAtLabel(4)) {
            liftTile()
        } else {
            phase = Phase.BringTile
        }
    }

    private fun getTileNWAfterLift() {
        if (carriesTile) {
            placeTile()
            return
        }
        phase = if (hasTileAtLabel(3) || hasTileAtLabel(2)) {
            moveToLabel(1)
            Phase.BringTile
        } else {
            moveToLabel(1)
            Phase.GetTileNW
        }
    }

    private fun bringTile() {
        if (!carriesTile) {
            liftTile()
            return
        }
        if (hasTileAtLabel(1)) {
            phase = Phase.BringTileMoveS
            return
        }
        intArrayOf(3, 2).forEach { label ->
            if (hasTileAtLabel(label)) {
                moveToLabel(label)
                return
            }
        }
    }

    private fun bringTileMoveS() {
        if (!hasTileAtLabel(2)) {
            moveToLabel(2)
            phase = Phase.BringTileFinalize
        } else {
            moveToLabel(3)
        }
    }

    private fun bringTileFinalize() {
        if (carriesTile) {
            placeTile()
            return
        }
        phase = if (hasTileAtLabel(3)) {
            Phase.Initialize
        } else {
            moveToLabel(4)
            Phase.GetTileN
        }
    }

}
