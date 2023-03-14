fun getRobot(orientation: Int, node: Node, carriesTile: Boolean, numPebbles: Int, maxPebbles: Int): Robot {
    return RobotImpl(orientation, node, carriesTile, numPebbles, maxPebbles)
}

class RobotImpl(
    orientation: Int,
    node: Node,
    carriesTile: Boolean,
    numPebbles: Int,
    maxPebbles: Int
): Robot(
    orientation = orientation,
    node = node,
    carriesTile = carriesTile,
    numPebbles = numPebbles,
    maxPebbles = maxPebbles
) {
    override fun activate() {
        TODO("Replace with your algorithm implementation")
    }
}
