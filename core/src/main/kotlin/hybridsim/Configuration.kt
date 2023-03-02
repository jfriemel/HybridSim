package hybridsim

object Configuration {

    var tiles: List<Tile> = listOf(Tile(0, 0), Tile(0, -1), Tile(0, 1), Tile(-1, 0), Tile(1, 0), Tile(1, -1), Tile(1, 1))

    init {
        tiles[1].addPebble()
    }

}
