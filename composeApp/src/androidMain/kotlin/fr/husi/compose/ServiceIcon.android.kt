package fr.husi.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import fr.husi.bg.ServiceState
import fr.husi.compose.material3.Icon
import fr.husi.lib.R

@Composable
actual fun ServiceIcon(state: ServiceState, contentDescription: String) {
    val icon = when (state) {
        ServiceState.Connecting, ServiceState.Stopping -> R.drawable.ic_service_busy
        else -> R.drawable.ic_service_idle
    }
    Icon(
        painterResource(icon),
        contentDescription,
    )
}
