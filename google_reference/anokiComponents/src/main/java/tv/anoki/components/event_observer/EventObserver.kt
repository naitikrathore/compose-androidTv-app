package tv.anoki.components.event_observer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Observes a Flow and triggers a callback whenever an event is emitted.
 *
 * @param flow The Flow instance that emits events, typically provided by a ViewModel.
 * @param key1 An optional key for triggering recomposition, usually a Compose state. Default is null.
 * @param key2 An optional key for triggering recomposition, usually a Compose state. Default is null.
 * @param onEvent A lambda function that will be called whenever an event is emitted by the flow.
 *
 * The keys are typically Compose states and are used to control when the composable should be recomposed.
 * In most cases, they will remain null.
 */
@Composable
fun <T> EventObserver(
    flow: Flow<T>, // Flow instance provided by the ViewModel
    key1: Any? = null, // Optional key for recomposition
    key2: Any? = null, // Optional key for recomposition
    onEvent: (T) -> Unit // Callback triggered on each emission
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Effect to observe the flow and handle lifecycle changes
    LaunchedEffect(flow, lifecycleOwner.lifecycle, key1, key2) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Switching to the main dispatcher for UI updates
            withContext(Dispatchers.Main.immediate) {
                // Collecting events emitted by the flow and invoking the callback
                flow.collect(onEvent)
            }
        }
    }
}