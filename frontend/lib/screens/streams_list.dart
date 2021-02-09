import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:frontend/dto/db_stream.dart';
import 'package:frontend/screens/add_update_stream.dart';
import 'package:frontend/service/db_stream_service.dart';
import 'package:frontend/service/service_locator.dart';

class DbStreamList extends StatefulWidget {
  final List<DbStream> dbStreams;

  const DbStreamList({Key key, this.dbStreams}) : super(key: key);

  @override
  _DbStreamListState createState() => _DbStreamListState();
}

class _DbStreamListState extends State<DbStreamList> {
  DbStreamService _streamsService = Services.get<DbStreamService>(DbStreamService);

  @override
  Widget build(BuildContext context) {
    if (widget.dbStreams.isEmpty) {
      return Center(child: Text("There is nothing to show"));
    }
    return ListView.builder(
        itemCount: widget.dbStreams.length,
        itemBuilder: (___, index) {
          final dbStream = widget.dbStreams[index];
          return Card(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                ListTile(
                  leading: Icon(Icons.album),
                  title: Text(dbStream.title),
                  subtitle: Text(dbStream.description ?? "..."),
                  trailing: PopupMenuButton<int>(
                    onSelected: (selected) {
                      switch (selected) {
                        case 1:
                          Navigator.of(context)
                              .push(MaterialPageRoute(builder: (__) => AddUpdateStream(dbStream: dbStream)));
                          break;
                        case 2:
                          _streamsService.delete(dbStream);
                          break;
                      }
                    },
                    child: Icon(Icons.more_vert),
                    itemBuilder: (context) => [
                      PopupMenuItem(
                        value: 1,
                        child: Text("Change"),
                      ),
                      PopupMenuItem(
                        value: 2,
                        child: Text("Delete"),
                      )
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                  child: Row(
                    mainAxisSize: MainAxisSize.max,
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: [
                      Text("Events: "),
                      ...dbStream.insert ? [Text("Insert"), Text("  |  ")] : [],
                      ...dbStream.update ? [Text("Update"), Text("  |  ")] : [],
                      ...dbStream.delete ? [Text("Delete")] : [],
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                  child: Row(
                    mainAxisSize: MainAxisSize.max,
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: [Text("Table: "), Text(dbStream.schema), Text("."), Text(dbStream.table)],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                  child: Row(
                    mainAxisSize: MainAxisSize.max,
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: [
                      Text("Topic: "),
                      Text(dbStream.topic),
                    ],
                  ),
                ),
              ],
            ),
          );
        });
  }
}
