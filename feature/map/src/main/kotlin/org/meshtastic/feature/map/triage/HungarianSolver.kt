package org.meshtastic.feature.map.triage

private const val INF = Double.MAX_VALUE / 2.0

object HungarianSolver {
    /**
     * Solves the assignment problem for a rectangular cost matrix using the
     * potential-based O(n³) Hungarian algorithm.
     *
     * @param cost  cost[i][j] = cost to assign worker i to job j.
     *              rows = numWorkers, cols = numJobs.
     * @return IntArray of length numWorkers where result[i] = column assigned to
     *         worker i, or -1 if the worker is unassigned (only when numWorkers > numJobs).
     */
    fun solve(cost: Array<DoubleArray>): IntArray {
        if (cost.isEmpty()) return IntArray(0)
        val numWorkers = cost.size
        val numJobs = cost[0].size
        if (numJobs == 0) return IntArray(numWorkers) { -1 }

        val n = maxOf(numWorkers, numJobs)

        val paddedCost = Array(n + 1) { DoubleArray(n + 1) }
        for (i in 1..n) {
            for (j in 1..n) {
                paddedCost[i][j] = when {
                    i <= numWorkers && j <= numJobs -> cost[i - 1][j - 1]
                    i <= numWorkers -> 0.0
                    else -> 0.0
                }
            }
        }

        val u = DoubleArray(n + 1)
        val v = DoubleArray(n + 1)
        val p = IntArray(n + 1)
        val way = IntArray(n + 1)

        for (i in 1..n) {
            p[0] = i
            var j0 = 0
            val minv = DoubleArray(n + 1) { INF }
            val used = BooleanArray(n + 1) { false }

            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = INF
                var j1 = -1

                for (j in 1..n) {
                    if (!used[j]) {
                        val cur = paddedCost[i0][j] - u[i0] - v[j]
                        if (cur < minv[j]) {
                            minv[j] = cur
                            way[j] = j0
                        }
                        if (minv[j] < delta) {
                            delta = minv[j]
                            j1 = j
                        }
                    }
                }

                for (j in 0..n) {
                    if (used[j]) {
                        u[p[j]] += delta
                        v[j] -= delta
                    } else {
                        minv[j] -= delta
                    }
                }

                j0 = j1
            } while (p[j0] != 0)

            do {
                val j1 = way[j0]
                p[j0] = p[j1]
                j0 = j1
            } while (j0 != 0)
        }

        val result = IntArray(numWorkers) { -1 }
        for (j in 1..n) {
            val workerIdx = p[j] - 1
            val jobIdx = j - 1
            if (workerIdx in 0 until numWorkers && jobIdx in 0 until numJobs) {
                result[workerIdx] = jobIdx
            }
        }
        return result
    }
}
