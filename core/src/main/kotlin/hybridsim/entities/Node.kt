package hybridsim.entities

data class Node(val x: Int, val y: Int) {
    fun nodeInDir(dir: Int): Node {
        assert(dir in 0..5)
        val xOffsets = if (y.mod(2) == 0) arrayOf(1, 1, 1, 0, -1, 0) else arrayOf(0, 1, 0, -1, -1, -1)
        val yOffsets = arrayOf(-1, 0, 1, 1, 0, -1)
        return Node(x + xOffsets[dir], y + yOffsets[dir])
    }

    companion object {
        val origin = Node(0, 0)
    }
}
