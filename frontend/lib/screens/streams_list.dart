import 'package:flutter/material.dart';
import 'package:frontend/dto/db_stream.dart';
import 'package:frontend/screens/add_stream.dart';

class DbStreamList extends StatefulWidget {
  final List<DbStream> dbStreams;

  const DbStreamList({Key key, this.dbStreams}) : super(key: key);

  @override
  _DbStreamListState createState() => _DbStreamListState();
}

class _DbStreamListState extends State<DbStreamList> {
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
                         break;
                      }
                    },
                    child: Icon(Icons.more_vert),
                    itemBuilder: (context) => [
                      PopupMenuItem(
                        value: 1,
                        child: Text("Alterar"),
                      ),
                      PopupMenuItem(
                        value: 2,
                        child: Text("Excluir"),
                      )
                    ],
                  ),
                ),
              ],
            ),
          );
        });
  }
}
