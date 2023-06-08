package org.runestar.cs2.util

typealias IntLoader<T> = Loader<Int, T>

fun interface Loader<K : Any, T : Any> {

    fun load(key: K): T?

    interface Keyed<K : Any, T : Any> : Loader<K, T> {

        val keys: Set<K>
    }

    data class Map<K : Any, T : Any>(val map: kotlin.collections.Map<K, T>) : Keyed<K, T> {

        override fun load(key: K): T? = map[key]

        override val keys: Set<K> get() = map.keys
    }

    data class Constant<K : Any, T : Any>(val t: T): Loader<K, T> {

        override fun load(key: K) = t
    }
}

fun <K : Any, T : Any> Loader<K, T>.caching(): Loader<K, T> = object : Loader<K, T> {

    private val cache = HashMap<K, T?>()

    override fun load(key: K): T? = cache[key] ?: if (key in cache) null else this@caching.load(key).also { cache[key] = it }
}

fun <T : Any> Loader<Int, T>.withIds(ids: Set<Int>): Loader.Keyed<Int, T> = object : Loader.Keyed<Int, T>, Loader<Int, T> by this {

    override val keys get() = ids
}

fun <K : Any, T : Any> Loader<K, T>.loadNotNull(key: K): T = checkNotNull(load(key)) { "Value for key $key was null" }

fun <K : Any, T : Any> Loader<K, T>.orElse(other: Loader<K, T>): Loader<K, T> = Loader { load(it) ?: other.load(it) }

fun <K : Any, T : Any, E : Any> Loader<K, T>.map(transform: (T) -> E): Loader<K, E> = Loader { load(it)?.let(transform) }

fun <T : Any, E : Any> Loader<Int, T>.mapIndexed(transform: (Int, T) -> E): Loader<Int, E> = Loader { id -> load(id)?.let { transform(id, it) } }


fun <T : Any> intLoader(t: T) = loader<Int, T>(t)

fun <K : Any, T : Any> loader(t: T) = Loader.Constant<K, T>(t)

fun <K : Any, T : Any> loader(map: Map<K, T>) = Loader.Map(map)
