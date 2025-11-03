package com.lucasjosino.on_audio_query.controllers

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.lucasjosino.on_audio_query.PluginProvider

/** OnPlaylistsController */
class PlaylistController {

    //Main parameters
    private val TAG = "OnPlaylistController"
    private val uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
    private val contentValues = ContentValues()
    private val channelError = "on_audio_error"
    private lateinit var resolver: ContentResolver

    //Query projection
    private val columns = arrayOf(
        "count(*)"
    )

    private val context = PluginProvider.context()
    private val result = PluginProvider.result()
    private val call = PluginProvider.call()

    //
    fun createPlaylist() {
        Log.i(TAG, "createPlaylist: Starting playlist creation")
        this.resolver = context.contentResolver
        val playlistName = call.argument<String>("playlistName")!!
        Log.i(TAG, "createPlaylist: Playlist name = $playlistName")

        try {
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Playlists.NAME, playlistName)
            contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis())
            Log.i(TAG, "createPlaylist: Inserting playlist into MediaStore")
            resolver.insert(uri, contentValues)
            Log.i(TAG, "createPlaylist: Playlist created successfully")
            result.success(true)
        } catch (e: Exception) {
            Log.i(channelError, "createPlaylist: Failed to create playlist - ${e.toString()}")
            result.success(false)
        }
    }

    //
    fun removePlaylist() {
        Log.i(TAG, "removePlaylist: Starting playlist removal")
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        Log.i(TAG, "removePlaylist: Playlist ID = $playlistId")

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) {
            Log.i(TAG, "removePlaylist: Playlist with ID $playlistId does not exist")
            result.success(false)
        } else {
            try {
                val delUri = ContentUris.withAppendedId(uri, playlistId.toLong())
                Log.i(TAG, "removePlaylist: Deleting playlist from MediaStore")
                resolver.delete(delUri, null, null)
                Log.i(TAG, "removePlaylist: Playlist removed successfully")
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, "removePlaylist: Failed to remove playlist - ${e.toString()}")
                result.success(false)
            }
        }
    }

    fun addToPlaylist() {
        Log.i(TAG, "addToPlaylist: Starting add audio to playlist")
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val audioId = call.argument<Int>("audioId")!!
        Log.i(TAG, "addToPlaylist: Playlist ID = $playlistId, Audio ID = $audioId")

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) {
            Log.i(TAG, "addToPlaylist: Playlist with ID $playlistId does not exist")
            result.success(false)
        } else {
            try {
                val membersUri =
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())
                Log.i(TAG, "addToPlaylist: Members URI = $membersUri")

                // Check if audio already exists in the playlist
                if (checkAudioInPlaylist(playlistId, audioId)) {
                    Log.i(TAG, "addToPlaylist: Audio $audioId already exists in playlist $playlistId")
                    result.success(false)
                    return
                }

                // compute play order: use max(play_order) + 1
                var playOrder = 0
                val projection = arrayOf("max(${MediaStore.Audio.Playlists.Members.PLAY_ORDER})")
                Log.i(TAG, "addToPlaylist: Querying for max play order")
                resolver.query(membersUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        playOrder = cursor.getInt(0)
                        Log.i(TAG, "addToPlaylist: Current max play order = $playOrder")
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, playOrder + 1)
                contentValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId)
                Log.i(TAG, "addToPlaylist: Inserting audio with play order ${playOrder + 1}")
                resolver.insert(membersUri, contentValues)
                Log.i(TAG, "addToPlaylist: Audio added to playlist successfully")
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, "addToPlaylist: Failed to add audio to playlist - ${e.toString()}")
                result.success(false)
            }
        }
    }

    // Helper function to check if audio already exists in playlist
    private fun checkAudioInPlaylist(playlistId: Int, audioId: Int): Boolean {
        Log.i(TAG, "checkAudioInPlaylist: Checking if audio $audioId exists in playlist $playlistId")
        val membersUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())
        val cursor = resolver.query(
            membersUri,
            arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
            "${MediaStore.Audio.Playlists.Members.AUDIO_ID}=?",
            arrayOf(audioId.toString()),
            null
        )
        cursor?.use {
            val exists = it.moveToFirst()
            Log.i(TAG, "checkAudioInPlaylist: Audio exists = $exists")
            return exists
        }
        Log.i(TAG, "checkAudioInPlaylist: Audio does not exist (cursor null)")
        return false
    }

    fun removeFromPlaylist() {
        Log.i(TAG, "removeFromPlaylist: Starting remove audio from playlist")
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val audioId = call.argument<Int>("audioId")!!
        Log.i(TAG, "removeFromPlaylist: Playlist ID = $playlistId, Audio ID = $audioId")

        //Check if Playlist exists based on Id
        if (!checkPlaylistId(playlistId)) {
            Log.i(TAG, "removeFromPlaylist: Playlist with ID $playlistId does not exist")
            result.success(false)
            return
        }

        try {
            val membersUri = MediaStore.Audio.Playlists.Members.getContentUri(
                "external",
                playlistId.toLong()
            )
            Log.i(TAG, "removeFromPlaylist: Members URI = $membersUri")
            
            // delete by AUDIO_ID so the audio (member) is removed from the playlist
            val where = "${MediaStore.Audio.Playlists.Members.AUDIO_ID}=?"
            val whereArgs = arrayOf(audioId.toString())

            // Try bulk delete first
            Log.i(TAG, "removeFromPlaylist: Attempting bulk delete")
            val deleted = resolver.delete(membersUri, where, whereArgs)
            if (deleted > 0) {
                Log.i(TAG, "removeFromPlaylist: Bulk delete successful - deleted $deleted row(s)")
                result.success(true)
                return
            }

            // If bulk delete didn't remove anything, query for member IDs and delete by member URI
            Log.i(TAG, "removeFromPlaylist: Bulk delete failed, trying individual member deletion")
            val projection = arrayOf(MediaStore.Audio.Playlists.Members._ID)
            val memberIds = mutableListOf<Long>()
            resolver.query(membersUri, projection, where, whereArgs, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)
                while (cursor.moveToNext()) {
                    if (idIndex >= 0) {
                        val memberId = cursor.getLong(idIndex)
                        memberIds.add(memberId)
                        Log.i(TAG, "removeFromPlaylist: Found member ID = $memberId")
                    }
                }
            }

            var anyDeleted = false
            for (memberId in memberIds) {
                val memberUri = ContentUris.withAppendedId(membersUri, memberId)
                Log.i(TAG, "removeFromPlaylist: Deleting member ID = $memberId")
                val delById = resolver.delete(memberUri, null, null)
                if (delById > 0) {
                    anyDeleted = true
                    Log.i(TAG, "removeFromPlaylist: Successfully deleted member ID = $memberId")
                }
            }

            Log.i(TAG, "removeFromPlaylist: Final result - anyDeleted = $anyDeleted")
            result.success(anyDeleted)
        } catch (e: Exception) {
            Log.i(channelError, "removeFromPlaylist: Failed to remove audio from playlist - ${e.toString()}")
            result.success(false)
        }
    }

    //TODO("Need tests")
    fun moveItemTo() {
        Log.i(TAG, "moveItemTo: Starting move item operation")
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val from = call.argument<Int>("from")!!
        val to = call.argument<Int>("to")!!
        Log.i(TAG, "moveItemTo: Playlist ID = $playlistId, from = $from, to = $to")

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) {
            Log.i(TAG, "moveItemTo: Playlist with ID $playlistId does not exist")
            result.success(false)
        } else {
            try {
                Log.i(TAG, "moveItemTo: Executing MediaStore move operation")
                MediaStore.Audio.Playlists.Members.moveItem(resolver, playlistId.toLong(), from, to)
                Log.i(TAG, "moveItemTo: Move operation completed successfully")
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, "moveItemTo: Failed to move item - ${e.toString()}")
                result.success(false)
            }
        }
    }

    //
    fun renamePlaylist() {
        Log.i(TAG, "renamePlaylist: Starting playlist rename")
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val newPlaylistName = call.argument<String>("newPlName")!!
        Log.i(TAG, "renamePlaylist: Playlist ID = $playlistId, new name = $newPlaylistName")

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) {
            Log.i(TAG, "renamePlaylist: Playlist with ID $playlistId does not exist")
            result.success(false)
        } else {
            try {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Playlists.NAME, newPlaylistName)
                contentValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis())
                // update via specific playlist Uri
                val updateUri = ContentUris.withAppendedId(uri, playlistId.toLong())
                Log.i(TAG, "renamePlaylist: Updating playlist in MediaStore")
                val updated = resolver.update(updateUri, contentValues, null, null)
                Log.i(TAG, "renamePlaylist: Update result - $updated row(s) updated")
                result.success(updated > 0)
            } catch (e: Exception) {
                Log.i(channelError, "renamePlaylist: Failed to rename playlist - ${e.toString()}")
                result.success(false)
            }
        }
    }

    //Return true if playlist already exist, false if don't exist
    private fun checkPlaylistId(plId: Int): Boolean {
        Log.i(TAG, "checkPlaylistId: Checking if playlist ID $plId exists")
        // query only the id and use selection to avoid iterating all rows
        val cursor = resolver.query(
            uri,
            arrayOf(MediaStore.Audio.Playlists._ID),
            "${MediaStore.Audio.Playlists._ID}=?",
            arrayOf(plId.toString()),
            null
        )
        cursor?.use {
            val exists = it.moveToFirst()
            Log.i(TAG, "checkPlaylistId: Playlist exists = $exists")
            return exists
        }
        Log.i(TAG, "checkPlaylistId: Playlist does not exist (cursor null)")
        return false
    }
}
