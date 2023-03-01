package hybridsim

class Tile(x: Int, y: Int) {
    private var numPebbles = 0

    fun addPebble() {
        numPebbles++
    }

    fun removePebble() {
        numPebbles--
    }

    fun hasPebble(): Boolean {
        return numPebbles > 0
    }
}
