package fr.husi.repository

import fr.husi.libcore.Service
import fr.husi.libcore.createBoxService
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import java.io.File
import org.jetbrains.compose.resources.getPluralString as getComposePluralString
import org.jetbrains.compose.resources.getString as getComposeString

fun resolveDesktopRepository(): DesktopRepository = resolveRepository() as DesktopRepository

class DesktopRepository(
    val dataDir: File,
) : Repository {

    override val isMainProcess: Boolean = true
    override val isBgProcess: Boolean = true
    override val isTv = false

    override val boxService: Service? by lazy {
        createBoxService(isBgProcess)
    }
    internal val serviceRuntime by lazy {
        DesktopServiceRuntime(boxService)
    }

    override val cacheDir: File by lazy {
        dataDir.resolve("cache").apply { mkdirs() }
    }

    override val filesDir: File by lazy {
        dataDir.resolve("files").apply { mkdirs() }
    }

    override val externalAssetsDir: File by lazy {
        dataDir.resolve("external").apply { mkdirs() }
    }

    override fun resolveDatabaseFile(name: String): File {
        return dataDir.resolve(name)
    }

    override suspend fun getString(resource: StringResource) = getComposeString(resource)
    override suspend fun getString(resource: StringResource, vararg formatArgs: Any) =
        getComposeString(resource, *formatArgs)

    override suspend fun getPluralString(
        resource: PluralStringResource,
        quantity: Int,
        vararg formatArgs: Any,
    ) = getComposePluralString(resource, quantity, *formatArgs)

    override fun startService() {
        serviceRuntime.start()
    }

    override fun reloadService() {
        serviceRuntime.reload()
    }

    override fun stopService() {
        serviceRuntime.stop()
    }
}
