package com.linkstar.visiongrader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linkstar.visiongrader.VisionGraderApplication
import com.linkstar.visiongrader.data.model.LoggedInUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object UserDataStore {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user")

    val USERNAME_KEY = stringPreferencesKey("USER_NAME")
    val PASSWORD_KEY = stringPreferencesKey("PASSWORD")
    val ACCESS_TOKEN_KEY = stringPreferencesKey("ACCESS_TOKEN")
    val USER_ID_KEY = stringPreferencesKey("USER_ID")
    val USER_NAME_KEY = stringPreferencesKey("USER_NAME")
    val SCHOOL_ID_KEY = stringPreferencesKey("SCHOOL_ID")
    val SCHOOL_NAME_KEY = stringPreferencesKey("SCHOOL_NAME")
    val SCHOOL_SHORT_NAME_KEY = stringPreferencesKey("SCHOOL_SHORT_NAME")
    val SCHOOL_CODE_KEY = stringPreferencesKey("SCHOOL_CODE")


    val userIdFlow: Flow<String?> = VisionGraderApplication.context.dataStore.data.map {
        it[USER_NAME_KEY]
    }

    val accessTokenFlow: Flow<String?> = VisionGraderApplication.context.dataStore.data.map {
        it[ACCESS_TOKEN_KEY]
    }



    suspend fun getUser(): LoggedInUser? {
        val store = VisionGraderApplication.context.dataStore.data.first()

        if (store[USER_ID_KEY] == null) return null

        return LoggedInUser(
                store[USERNAME_KEY]!!,
                store[PASSWORD_KEY]!!,
                store[ACCESS_TOKEN_KEY]!!,
                store[USER_ID_KEY]!!,
                store[USER_NAME_KEY]!!,
                store[SCHOOL_ID_KEY]!!,
                store[SCHOOL_CODE_KEY]!!,
                store[SCHOOL_NAME_KEY]!!,
                store[SCHOOL_SHORT_NAME_KEY]!!
            )
    }


    suspend fun storeUser(data: LoggedInUser) {
        VisionGraderApplication.context.dataStore.edit { user ->
            user[USERNAME_KEY] = data.username
            user[PASSWORD_KEY] = data.password

            user[ACCESS_TOKEN_KEY] = data.accessToken
            user[USER_ID_KEY] = data.userId
            user[USER_NAME_KEY] = data.name

            user[SCHOOL_ID_KEY] = data.schoolId
            user[SCHOOL_NAME_KEY] = data.schoolName
            user[SCHOOL_SHORT_NAME_KEY] = data.schoolShortName
            user[SCHOOL_CODE_KEY] = data.schoolCode
        }
    }


    suspend fun clearUser() {
        VisionGraderApplication.context.dataStore.edit { user ->
            user.remove(USERNAME_KEY)
            user.remove(PASSWORD_KEY)

            user.remove(ACCESS_TOKEN_KEY)
            user.remove(USER_ID_KEY)
            user.remove(USER_NAME_KEY)
        }
    }
}