package hybridsim.entities

data class Node(val x: Int, val y: Int) {

    /**
     * Find the node in the given global direction.
     * Directions start at N and go clockwise: 0 = N, 1 = NE, 2 = SE, 3 = S, 4 = SW, 5 = NW.
     *
     * @param dir Global direction.
     * @return The node in the given global direction.
     */
    fun nodeInDir(dir: Int): Node {
        assert(dir in 0..5)
        val xOffsets = arrayOf(0, 1, 1, 0, -1, -1)
        val yOffsets = if (x.mod(2) == 0) arrayOf(-1, 0, 1, 1, 1, 0) else arrayOf(-1, -1, 0, 1, 0, -1)
        return Node(x + xOffsets[dir], y + yOffsets[dir])
    }

    /**
     * Convert the node's internal integer coordinates to scientific coordinates as used in the hybrid model literature.
     * See: https://doi.org/10.1007/s11047-019-09774-2
     *
     * @return The node's scientific coordinates in form (x, y).
     */
    fun scientificCoordinates(): Pair<Double, Double> {
        val scx = x.toDouble()
        val scy = y.toDouble() - if (x.mod(2) == 0) 0.0 else 0.5
        return Pair(scx, scy)
    }

    companion object {
        val origin = Node(0, 0)

        /**
         * Returns a node corresponding to the given scientific node coordinates.
         *
         * @param scx Scientific x coordinate.
         * @param scy Scientific y coordinate.
         * @return The node corresponding to the given scientific node coordinates.
         */
        fun sciCoordsToNode(scx: Double, scy: Double): Node {
            val x = scx.toInt()
            val y = (scy + if (x.mod(2) == 0) 0.0 else 0.5).toInt()
            return Node(x, y)
        }
    }
}
