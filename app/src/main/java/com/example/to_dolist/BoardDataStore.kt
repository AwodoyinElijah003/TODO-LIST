package com.example.to_dolist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.dataStore by preferencesDataStore(name = "boards_prefs")

class BoardDataStore(private val context: Context) {
    private val gson = Gson()
    private val BOARDS_KEY = stringPreferencesKey("boards_json")

    // Save boards list
    suspend fun saveBoards(boards: List<Board>) {
        val serializable = boards.map { it.toSerializable() } // convert for storage
        val json = gson.toJson(serializable)
        context.dataStore.edit { prefs ->
            prefs[BOARDS_KEY] = json
        }
    }

    // Get boards list
    fun getBoards(): Flow<List<Board>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[BOARDS_KEY]
            if (json != null) {
                try {
                    val type = object : TypeToken<List<SerializableBoard>>() {}.type
                    val serializableList: List<SerializableBoard> = gson.fromJson(json, type)
                    serializableList.map { it.toDomain() }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
}
