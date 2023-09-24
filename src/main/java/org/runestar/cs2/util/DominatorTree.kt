package org.runestar.cs2.util

fun <N : Any> dominatorTree(graph: DirectedGraph<N>): DirectedGraph<N> {
    // A Simple, Fast Dominance Algorithm. Cooper, Harvey, Kennedy. 2001
    val postOrderList = ArrayList<N>()
    val postOrderMap = HashMap<N, Int>()
    graph.postOrder {
        postOrderMap[it] = postOrderList.size
        postOrderList.add(it)
    }

    val idoms = HashMap<N, N>()

    fun post(finger: N)= postOrderMap[finger] ?: error("Missing from post order map: $finger")

    fun intersect(pred: N, idom: N): N {
        if (!postOrderMap.contains(pred) || !postOrderMap.contains(idom)){
            return pred
        }
        var f1 = pred
        var f2 = idom
        while (f1 != f2) {
            while (post(f1) < post(f2)) {
                val value = idoms.getValue(f1)
                if (!postOrderMap.contains(value)){
                    break
                }
                f1 = value
            }
            while (post(f2) < post(f1)) {
                val value = idoms.getValue(f2)
                if (!postOrderMap.contains(value)){
                    break
                }
                f2 = value
            }
        }
        return f1
    }

//    idoms[graph.head] = graph.head
    var changed: Boolean
    do {
        changed = false
        for (i in (postOrderList.lastIndex - 1) downTo 0) {
            val n = postOrderList[i]
            val predecessors = graph.immediatePredecessors(n)
            var newIdom = predecessors[0]
            for (pi in 1..predecessors.lastIndex) {
                val p = predecessors[pi]
                if (p in idoms) {
                    newIdom = intersect(p, newIdom)
                }
            }
            changed = idoms.put(n, newIdom) != newIdom
        }
    } while (changed)

    val dominatorTree = LinkedGraph<N>()
    for ((n, idom) in idoms) {
        dominatorTree.addSuccessor(idom, n)
    }
    dominatorTree.head = graph.head
    return dominatorTree
}

