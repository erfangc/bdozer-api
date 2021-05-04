package bdozer.api.common.extensions

object DoubleExtensions {

    fun Double?.orZero() = this ?: 0.0

}