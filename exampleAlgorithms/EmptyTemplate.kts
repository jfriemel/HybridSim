fun getRobot(orientation: Int, node: Node): Robot {
    return RobotImpl(orientation, node)
}

class RobotImpl(orientation: Int, node: Node): Robot(
    orientation = orientation,  // Replace with constant (0-5) if you want the robots to share a compass
    node = node,
    carriesTile = false,  // Change constants to fit your needs
    numPebbles = 2,  // Change constants to fit your needs
    maxPebbles = 2  // Change constants to fit your needs
) {
    override fun activate() {
        TODO("Replace with your algorithm implementation")
    }
}
