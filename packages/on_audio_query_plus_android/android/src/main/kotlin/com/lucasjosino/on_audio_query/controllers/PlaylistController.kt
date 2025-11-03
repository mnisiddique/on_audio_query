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
        this.resolver = context.contentResolver
        val playlistName = call.argument<String>("playlistName")!!

        try {
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Playlists.NAME, playlistName)
            contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis())
            resolver.insert(uri, contentValues)
            result.success(true)
        } catch (e: Exception) {
            Log.i(channelError, e.toString())
            result.success(false)
        }
    }

    //
    fun removePlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            try {
                val delUri = ContentUris.withAppendedId(uri, playlistId.toLong())
                resolver.delete(delUri, null, null)
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, e.toString())
                result.success(false)
            }
        }
    }

    //TODO Add option to use a list
    //TODO Fix error on Android 10
    fun addToPlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val audioId = call.argument<Int>("audioId")!!


        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            try {
                val membersUri =
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())

                // compute play order: use max(play_order) + 1
                var playOrder = 0
                val projection = arrayOf("max(${MediaStore.Audio.Playlists.Members.PLAY_ORDER})")
                resolver.query(membersUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        playOrder = cursor.getInt(0)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, playOrder + 1)
                // AUDIO_ID is an integer column; put as Long or Int is acceptable
                contentValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId)
                //contentValues.put(MediaStore.Audio.AudioColumns._ID, audioId)
                resolver.insert(membersUri, contentValues)
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, e.toString())
                result.success(false)
            }
        }
    }

    //TODO Add option to use a list
    fun removeFromPlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val audioId = call.argument<Int>("audioId")!!

        //Check if Playlist exists based on Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            try {
                val uri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external",
                    playlistId.toLong()
                )
                // delete by AUDIO_ID so the audio (member) is removed from the playlist
                val where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?"
                resolver.delete(uri, where, arrayOf(audioId.toString()))
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, e.toString())
                result.success(false)
            }
        }
    }

    //TODO("Need tests")
    fun moveItemTo() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val from = call.argument<Int>("from")!!
        val to = call.argument<Int>("to")!!

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            try {
                MediaStore.Audio.Playlists.Members.moveItem(resolver, playlistId.toLong(), from, to)
                result.success(true)
            } catch (e: Exception) {
                Log.i(channelError, e.toString())
                result.success(false)
            }
        }
    }

    //
    fun renamePlaylist() {
        this.resolver = context.contentResolver
        val playlistId = call.argument<Int>("playlistId")!!
        val newPlaylistName = call.argument<String>("newPlName")!!

        //Check if Playlist exists based in Id
        if (!checkPlaylistId(playlistId)) result.success(false)
        else {
            try {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Playlists.NAME, newPlaylistName)
                contentValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis())
                // update via specific playlist Uri
                val updateUri = ContentUris.withAppendedId(uri, playlistId.toLong())
                val updated = resolver.update(updateUri, contentValues, null, null)
                result.success(updated > 0)
            } catch (e: Exception) {
                Log.i(channelError, e.toString())
                result.success(false)
            }
        }
    }

    //Return true if playlist already exist, false if don't exist
    private fun checkPlaylistId(plId: Int): Boolean {
        // query only the id and use selection to avoid iterating all rows
        val cursor = resolver.query(
            uri,
            arrayOf(MediaStore.Audio.Playlists._ID),
            "${MediaStore.Audio.Playlists._ID}=?",
            arrayOf(plId.toString()),
            null
        )
        cursor?.use {
            return it.moveToFirst()
        }
        return false
    }
}

//Extras:

//I/PlaylistCursor[All]: [
//  title_key
// instance_id
// playlist_id
// duration
// is_ringtone
// album_artist
// orientation
// artist
// height
// is_drm
// bucket_display_name
// is_audiobook
// owner_package_name
// volume_name
// title_resource_uri
// date_modified
// date_expires
// composer
// _display_name
// datetaken
// mime_type
// is_notification
// _id
// year
// _data
// _hash
// _size
// album
// is_alarm
// title
// track
// width
// is_music
// album_key
// is_trashed
// group_id
// document_id
// artist_id
// artist_key
// is_pending
// date_added
// audio_id
// is_podcast
// album_id
// primary_directory
// secondary_directory
// original_document_id
// bucket_id
// play_order
// bookmark
// relative_path
// ]