/*
=============
Author: Lucas Josino
Github: https://github.com/LucJosin
Website: https://www.lucasjosino.com/
=============
Plugin/Id: on_audio_query#0
Homepage: https://github.com/LucJosin/on_audio_query
Pub: https://pub.dev/packages/on_audio_query
License: https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/LICENSE
Copyright: © 2021, Lucas Josino. All rights reserved.
=============
*/

import 'package:flutter/material.dart';
import 'package:on_audio_query_example/playlist_page.dart';
import 'package:on_audio_query_example/songs_page.dart';

void main() {
  runApp(
    const MaterialApp(
      home: HomePage(),
    ),
  );
}

class HomePage extends StatelessWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Music Library'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Songs', icon: Icon(Icons.music_note)),
              Tab(text: 'Playlists', icon: Icon(Icons.playlist_play)),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            SongsPage(),
            PlaylistPage(),
          ],
        ),
      ),
    );
  }
}
