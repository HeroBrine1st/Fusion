package ru.herobrine1st.fusion.database

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Dispatchers.Database get() = IO

suspend fun <T : Any> Query<T>.awaitAsOne() = withContext(Dispatchers.Database) {
    executeAsOne()
}

suspend fun <T : Any> Query<T>.awaitAsOneOrNull() = withContext(Dispatchers.Database) {
    executeAsOneOrNull()
}

suspend fun <T : Any> Query<T>.awaitAsList() = withContext(Dispatchers.Database) {
    executeAsList()
}
suspend fun ExecutableQuery<*>.await(): Unit = withContext(Dispatchers.Database) {
    execute {}
}
suspend fun <T : Any> ExecutableQuery<T>.awaitAsOne() = withContext(Dispatchers.Database) {
    executeAsOne()
}

suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneOrNull() = withContext(Dispatchers.Database) {
    executeAsOneOrNull()
}

suspend fun <T : Any> ExecutableQuery<T>.awaitAsList() = withContext(Dispatchers.Database) {
    executeAsList()
}