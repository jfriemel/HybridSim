package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Test [Node] functionality. */
class TestNode {

    @ParameterizedTest(name = "direction {0}")
    @CsvSource(
        "0, 2, 6",
        "1, 3, 7",
        "2, 3, 8",
        "3, 2, 8",
        "4, 1, 8",
        "5, 1, 7",
    )
    fun `neighbors on even column`(dir: Int, nbrX: Int, nbrY: Int) {
        val node = Node(2, 7)
        Assertions.assertEquals(Node(nbrX, nbrY), node.nodeInDir(dir))
        Assertions.assertEquals(6, node.neighbors().size)
        Assertions.assertTrue(Node(nbrX, nbrY) in node.neighbors())
    }

    @ParameterizedTest(name = "direction {0}")
    @CsvSource(
        "0, -3, -5",
        "1, -2, -5",
        "2, -2, -4",
        "3, -3, -3",
        "4, -4, -4",
        "5, -4, -5",
    )
    fun `neighbors on odd column`(dir: Int, nbrX: Int, nbrY: Int) {
        val node = Node(-3, -4)
        Assertions.assertEquals(Node(nbrX, nbrY), node.nodeInDir(dir))
        Assertions.assertEquals(6, node.neighbors().size)
        Assertions.assertTrue(Node(nbrX, nbrY) in node.neighbors())
    }

}
