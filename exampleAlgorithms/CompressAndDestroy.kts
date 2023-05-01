/**
 * The algorithm assumes that the robot is on the outer tile boundary and not adjacent to any other boundary.
 * The robot removes all tiles by destroying safely removable tiles and moving safely movable tiles away from the outer
 * boundary, filling holes, until all tiles are destroyed.
 */

import com.github.jfriemel.hybridsim.system.Configuration

fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

private enum class Phase {
    Initialize,
    SearchAndDestroy,
    Compress,
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

    private var outerLabel = -1
    private var compressDir = -1
    private var hasMoved = false

    override fun activate() {
        when (phase) {
            Phase.Initialize -> initialize()
            Phase.SearchAndDestroy -> searchAndDestroy()
            Phase.Compress -> compress()
            Phase.Finished -> return
        }
    }

    override fun finished(): Boolean = phase == Phase.Finished

    override fun getColor(): Color = when (phase) {
        Phase.Initialize -> Color.ORANGE
        Phase.SearchAndDestroy -> Color.SKY
        Phase.Compress -> Color.TEAL
        Phase.Finished -> Color.BLACK
    }

    /**
     * Enter phase: [Phase.Initialize]
     *
     * The robot sets a pointer to the outer boundary. If it has no empty node neighbor, it moves in direction 0 until
     * it finds one.
     *
     * Exit phase: [Phase.SearchAndDestroy]
     */
    private fun initialize() {
        labels.forEach { label ->
            if (!hasTileAtLabel(label)) {
                outerLabel = label
                phase = Phase.SearchAndDestroy
                return
            }
        }
        moveToLabel(0)
    }

    /**
     * Enter phase: [Phase.SearchAndDestroy]
     *
     * Whenever the robot finds a safely removable tile, it destroys it.
     * Whenever the robot finds a tile that can be moved towards the inner boundary, it lifts it and enters
     * [Phase.Compress].
     * The robot traverses the tile boundary until no tiles are left, then it enters [Phase.Finished].
     *
     * Exit phases:
     *   [Phase.Compress]
     *   [Phase.Finished]
     */
    private fun searchAndDestroy() {
        if (isOnTile() && isAtEdge()) {
            Configuration.removeTile(node)
            return
        }
        val moveLabel = (1..6).map { (outerLabel + it).mod(6) }.firstOrNull { label ->
            hasTileAtLabel(label)
        }
        if (moveLabel == null) {
            phase = Phase.Finished
            return
        }
        if (!hasTileAtLabel((moveLabel + 1).mod(6)) && hasTileAtLabel((moveLabel + 2).mod(6))) {
            liftTile()
            compressDir = (moveLabel + 1).mod(6)
            hasMoved = false
            phase = Phase.Compress
            return
        }
        moveToLabel(moveLabel)
        outerLabel = (moveLabel - 2).mod(6)
    }

    /**
     * Enter phase: [Phase.Compress]
     *
     * The robot moves the carried tile towards the inner boundary, places it, moves back, and switches back to
     * [Phase.SearchAndDestroy].
     *
     * Exit phase: [Phase.SearchAndDestroy]
     */
    private fun compress() {
        if (!hasMoved) {
            moveToLabel(compressDir)
            hasMoved = true
            return
        }
        if (carriesTile) {
            placeTile()
            return
        }
        moveToLabel((compressDir + 2).mod(6))
        outerLabel = (compressDir + 4).mod(6)
        phase = Phase.SearchAndDestroy
    }

    /**
     * Helper function
     *
     * Checks whether the robot is at an edge of the tile structure.
     */
    private fun isAtEdge(): Boolean {
        val boundaryLabels = labels.filter { label -> !hasTileAtLabel(label) }

        if (boundaryLabels.size == 6) {
            return true
        }

        var numBoundaries = 0
        boundaryLabels.forEach { label ->
            if ((label + 1).mod(6) !in boundaryLabels) {
                numBoundaries++
            }
        }
        return numBoundaries == 1
    }
}
