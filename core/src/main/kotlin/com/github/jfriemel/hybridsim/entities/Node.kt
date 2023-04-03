package com.github.jfriemel.hybridsim.entities

data class Node(val x: Int, val y: Int) {

    /**
     * Finds and returns the [Node] in the given global direction [dir].
     * Directions start at N and go clockwise: 0 = N, 1 = NE, 2 = SE, 3 = S, 4 = SW, 5 = NW.
     */
    fun nodeInDir(dir: Int): Node {
        assert(dir in 0..5)
        val xOffsets = arrayOf(0, 1, 1, 0, -1, -1)
        val yOffsets = if (x.mod(2) == 0) arrayOf(-1, 0, 1, 1, 1, 0) else arrayOf(-1, -1, 0, 1, 0, -1)
        return Node(x + xOffsets[dir], y + yOffsets[dir])
    }

    /**
     * Converts the node's internal integer coordinates to scientific coordinates as used in the hybrid model
     * literature. Returns scientific coordinates as a [Pair] (x, y).
     * See: https://doi.org/10.1007/s11047-019-09774-2
     */
    fun scientificCoordinates(): Pair<Double, Double> {
        val scX = x.toDouble()
        val scY = y.toDouble() - if (x.mod(2) == 0) 0.0 else 0.5
        return Pair(scX, scY)
    }

    companion object {
        val origin = Node(0, 0)

        /** @return A [Node] corresponding to the given scientific node coordinates ([scX], [scY]). */
        @Suppress("Unused")
        fun sciCoordsToNode(scX: Double, scY: Double): Node {
            val x = scX.toInt()
            val y = (scY + if (x.mod(2) == 0) 0.0 else 0.5).toInt()
            return Node(x, y)
        }
    }
}
