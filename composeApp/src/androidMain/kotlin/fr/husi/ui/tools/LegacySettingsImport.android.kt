package fr.husi.ui.tools

import android.os.Parcel
import fr.husi.database.DataStore
import fr.husi.database.preference.KeyValuePair
import fr.husi.database.preference.importLegacyPairs
import fr.husi.ktx.b64Decode
import kotlinx.parcelize.parcelableCreator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

internal actual suspend fun importLegacySettingPairs(rawSettings: Any) {
    val array = rawSettings as? JsonArray
        ?: throw IllegalArgumentException("Legacy settings must be a JSON array")
    val pairs = mutableListOf<KeyValuePair>()
    for ((index, element) in array.withIndex()) {
        val primitive = element as? JsonPrimitive
            ?: throw IllegalArgumentException("Legacy settings entry $index is not a primitive")
        val data = try {
            primitive.content.b64Decode()
        } catch (e: Exception) {
            throw IllegalArgumentException("Legacy settings entry $index has invalid Base64", e)
        }
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
            pairs.add(parcelableCreator<KeyValuePair>().createFromParcel(parcel))
        } finally {
            parcel.recycle()
        }
    }
    DataStore.configurationStore.importLegacyPairs(pairs)
}
