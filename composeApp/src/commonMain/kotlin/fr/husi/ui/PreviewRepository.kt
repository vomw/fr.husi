package fr.husi.ui

import fr.husi.di.initHusiKoin
import fr.husi.repository.FakeRepository
import org.koin.core.context.GlobalContext

internal fun ensurePreviewRepository() {
    if (GlobalContext.getOrNull() == null) {
        initHusiKoin(FakeRepository())
    }
}
