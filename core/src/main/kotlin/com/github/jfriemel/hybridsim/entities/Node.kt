package com.github.jfriemel.hybridsim.entities

data class Node(val x: Int, val y: Int) {

    /**
     * Finds and returns the [Node] in the given global direction [dir]. Directions start at N and
     * go clockwise: 0 = N, 1 = NE, 2 = SE, 3 = S, 4 = SW, 5 = NW.
     */
    fun nodeInDir(dir: Int): Node {
        assert(dir in 0..5)
        val xOffsets = arrayOf(0, 1, 1, 0, -1, -1)
        val yOffsets =
            if (x.mod(2) == 0) arrayOf(-1, 0, 1, 1, 1, 0) else arrayOf(-1, -1, 0, 1, 0, -1)
        return Node(x + xOffsets[dir], y + yOffsets[dir])
    }

    /** All six neighbors of the current [Node]. */
    fun neighbors(): Collection<Node> = (0..5).map { dir -> nodeInDir(dir) }

    companion object {
        val origin = Node(0, 0)
    }
}
