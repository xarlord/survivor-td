package com.survivortd.game.utils

/**
 * Generic object pool to avoid GC pressure from frequent create/destroy cycles.
 * Pre-allocates [initialSize] objects and reuses them via acquire/release.
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val initialSize: Int = 100,
    private val maxSize: Int = 500
) {
    private val pool = ArrayDeque<T>(initialSize)

    init {
        repeat(initialSize) { pool.addLast(factory()) }
    }

    /** Acquire an object from the pool (creates new if empty). */
    fun acquire(): T {
        return if (pool.isNotEmpty()) {
            val item = pool.removeLast()
            reset(item)
            item
        } else {
            factory()
        }
    }

    /** Return an object to the pool for reuse. */
    fun release(item: T) {
        if (pool.size < maxSize) {
            reset(item)
            pool.addLast(item)
        }
    }

    /** Current number of objects available in the pool. */
    val size: Int get() = pool.size
}
