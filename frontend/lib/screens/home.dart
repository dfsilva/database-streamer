import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:frontend/dto/db_stream.dart';
import 'package:frontend/screens/add_update_stream.dart';
import 'package:frontend/screens/streams_list.dart';
import 'package:frontend/service/db_stream_service.dart';
import 'package:frontend/service/service_locator.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  DbStreamService _streamsService = Services.get<DbStreamService>(DbStreamService);

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text("Table Streams")),
        body: FutureBuilder(
          future: _streamsService.dbStreams(),
          builder: (_, AsyncSnapshot<List<DbStream>> snp) {
            if (snp.hasError) {
              return Center(
                child: Text("Houston! We have a problem!"),
              );
            }

            if (!snp.hasData) {
              return Center(
                child: Text("Loading..."),
              );
            }

            return Observer(builder: (_) {
              List<DbStream> _streams = _streamsService.store().dbStreams.values.toList();
              return DbStreamList(dbStreams: _streams);
            });
          },
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            Navigator.of(context).push(MaterialPageRoute(builder: (_) => AddUpdateStream()));
          },
          child: Icon(Icons.add),
        ),
      );
}
