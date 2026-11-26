package fr.husi.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.husi.bg.ServiceStatus
import fr.husi.compose.material3.Text
import fr.husi.ktx.formatBytes
import fr.husi.ktx.readableUrlTestError
import fr.husi.resources.Res
import fr.husi.resources.connection_test_available
import fr.husi.resources.connection_test_available_http
import fr.husi.resources.connection_test_error
import fr.husi.resources.connection_test_testing
import fr.husi.resources.speed
import fr.husi.resources.vpn_connected
import fr.husi.ui.URLTestStatus
import org.jetbrains.compose.resources.stringResource

/**
 * A connected-state overlay that displays proxy traffic and tests the active
 * connection when tapped.
 */
@Composable
fun ConnectionStatsBar(
    status: ServiceStatus,
    urlTestStatus: URLTestStatus,
    isHttpsTest: Boolean,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status.state.connected) {
        Surface(
            onClick = onTestConnection,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    text = "▲ " + stringResource(
                        Res.string.speed,
                        (status.speed?.txRateProxy ?: 0L).formatBytes(),
                    ),
                )
                Text(
                    text = "▼ " + stringResource(
                        Res.string.speed,
                        (status.speed?.rxRateProxy ?: 0L).formatBytes(),
                    ),
                )
                Text(
                    text = when (urlTestStatus) {
                        URLTestStatus.Initial -> stringResource(Res.string.vpn_connected)
                        URLTestStatus.Testing -> stringResource(Res.string.connection_test_testing)
                        is URLTestStatus.Success -> stringResource(
                            if (isHttpsTest) {
                                Res.string.connection_test_available
                            } else {
                                Res.string.connection_test_available_http
                            },
                            urlTestStatus.delay,
                        )
                        is URLTestStatus.Exception -> {
                            val readableError = readableUrlTestError(urlTestStatus.message)
                            stringResource(
                                Res.string.connection_test_error,
                                readableError?.let { stringResource(it) } ?: urlTestStatus.message,
                            )
                        }
                    },
                )
            }
        }
    }
}
