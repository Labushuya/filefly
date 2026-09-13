package de.filefly.core.network

// Ergebnis-Wrapper für API-Aufrufe: entweder Nutzlast oder ein strukturierter Fehler.
// Kein Werfen quer durch die Layer — der Aufrufer (ViewModel) entscheidet über UI.
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>

    data class Failure(
        val code: Int,
        val message: String,
    ) : ApiResult<Nothing>

    companion object {
        // Netzwerk-/IO-Fehler ohne HTTP-Status.
        fun networkError(message: String): Failure = Failure(code = -1, message = message)
    }
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> =
    when (this) {
        is ApiResult.Ok -> ApiResult.Ok(transform(value))
        is ApiResult.Failure -> this
    }

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Ok)?.value
