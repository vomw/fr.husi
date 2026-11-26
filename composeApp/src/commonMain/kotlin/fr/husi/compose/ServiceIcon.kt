package fr.husi.compose

import androidx.compose.runtime.Composable
import fr.husi.bg.ServiceState

@Composable
expect fun ServiceIcon(state: ServiceState, contentDescription: String)
