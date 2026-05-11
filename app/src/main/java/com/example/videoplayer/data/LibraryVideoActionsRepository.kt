package com.example.videoplayer.data

import com.example.videoplayer.data.local.FavoriteVideoUriEntity
import com.example.videoplayer.data.local.IgnoredVideoUriEntity
import com.example.videoplayer.data.local.LibraryVideoActionsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryVideoActionsRepository(private val dao: LibraryVideoActionsDao) {

    fun observeIgnoredUris(): Flow<Set<String>> =
        dao.observeIgnoredUris().map { it.toSet() }

    fun observeFavoriteUris(): Flow<Set<String>> =
        dao.observeFavoriteUris().map { it.toSet() }

    suspend fun ignoreUri(uri: String) {
        dao.addIgnored(IgnoredVideoUriEntity(contentUri = uri))
    }

    suspend fun favoriteUri(uri: String) {
        dao.addFavorite(FavoriteVideoUriEntity(contentUri = uri))
    }

    suspend fun unfavoriteUri(uri: String) {
        dao.removeFavorite(uri)
    }
}
