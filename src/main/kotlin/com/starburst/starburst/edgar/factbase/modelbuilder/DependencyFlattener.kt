package com.starburst.starburst.edgar.factbase.modelbuilder

import java.util.*

/**
 * Flattens out the dependency as specified by input
 * [input] specifies the immediate dependency nature between items
 */
class DependencyFlattener(private val input: Map<String, List<String>>) {
    fun flatten(): Map<String, Set<String>> {
        val result = mutableMapOf<String, Set<String>>()

        for (entry in input) {
            /*
            use a hash set o hold the flattened list of dependencies
            just in case of duplicates
             */
            val deps = hashSetOf<String>()

            /*
            use a stack to hold items to be processed
            in a DFS fashion
             */
            val stack = Stack<String>()
            stack.addAll(entry.value)
            while (stack.isNotEmpty()) {
                val dep = stack.pop()
                val immediateDeps = input[dep]
                if (immediateDeps.isNullOrEmpty()) {
                    // root found, stopping condition reached
                    deps.add(dep)
                } else {
                    stack.addAll(immediateDeps)
                }
            }
            result[entry.key] = deps
        }
        return result
    }
}