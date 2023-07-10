fun getGenerator(): Generator = GeneratorImpl()

class GeneratorImpl(): Generator() {

    override fun generate(numTiles: Int, numRobots: Int, numOverhang: Int): ConfigurationDescriptor {
        val descriptor = ConfigurationDescriptor(mutableSetOf(), mutableSetOf(), mutableSetOf())

        if (numTiles <= 0) {
            return descriptor
        }

        // Tiles
        descriptor.tileNodes.add(Node.origin)
        val tileCandidates = Node.origin.neighbors().toMutableSet()
        repeat(numTiles + numTiles / 2 - 1) {
            val nextNode = tileCandidates.filter { node -> isValidCandidate(node, descriptor.tileNodes) }.random()
            descriptor.tileNodes.add(nextNode)
            tileCandidates.addAll(
                nextNode.neighbors().minus(descriptor.tileNodes)
            )
            tileCandidates.remove(nextNode)
        }
        val tileRemoveCandidates = mutableSetOf<Node>()
        tileCandidates.forEach { node -> tileRemoveCandidates.addAll(node.neighbors()) }
        repeat(numTiles / 2) {
            val nextNode = tileRemoveCandidates.filter { node -> isValidCandidate(node, descriptor.tileNodes) }.random()
            descriptor.tileNodes.remove(nextNode)
            tileRemoveCandidates.addAll(
                nextNode.neighbors().intersect(descriptor.tileNodes)
            )
            tileRemoveCandidates.remove(nextNode)
        }

        return descriptor
    }

    private fun isValidCandidate(candidate: Node, nodeSet: Set<Node>): Boolean {
        val otherDirs = (0..5).filter { dir -> candidate.nodeInDir(dir) !in nodeSet }
        return otherDirs.size == 6 || otherDirs.filter { label -> (label + 1).mod(6) !in otherDirs }.size == 1
    }

}
