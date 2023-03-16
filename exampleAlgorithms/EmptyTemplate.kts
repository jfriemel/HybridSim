fun getRobot(orientation: Int, node: Node): Robot {
    return RobotImpl(orientation, node)
}

class RobotImpl(orientation: Int, node: Node): Robot(
    orientation = orientation,
    node = node,
    carriesTile = false,
    numPebbles = 2,
    maxPebbles = 2
) {
    override fun activate() {
        TODO("Replace with your algorithm implementation")
    }
}
