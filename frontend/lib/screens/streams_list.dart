import 'package:code_editor/code_editor.dart';
import 'package:flutter/material.dart';
import 'package:frontend/dto/db_stream.dart';


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
                  title: Row(
                    children: [Text(dbStream.title), Text(dbStream.agentType == "D" ? " - (Default)" : " - (Conditional)")],
                  ),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [Text(dbStream.description, style: TextStyle(fontSize: 10)), Text(dbStream.uuid, style: TextStyle(fontSize: 10)), Text("Ordered: ${dbStream.ordered}")],
                  ),
                  trailing: PopupMenuButton<int>(
                    onSelected: (selected) {
                      switch (selected) {
                        case 1:
                          Navigator.of(context).push(MaterialPageRoute(builder: (__) => AddAgentScreen(agent: dbStream)));
                      }
                    },
                    child: Icon(Icons.more_vert),
                    itemBuilder: (context) => [
                      PopupMenuItem(
                        value: 1,
                        child: Text("Alterar"),
                      )
                    ],
                  ),
                ),
                ...(dbStream.agentType == "C")
                    ? [
                        Text("Conditional Script"),
                        Container(
                          height: 100,
                          child: SingleChildScrollView(
                            child: CodeEditor(
                              model: EditorModel(
                                files: [FileEditor(name: "codigo", language: "java", code: dbStream.ifscript)],
                                styleOptions: EditorModelStyleOptions(
                                  fontSize: 10,
                                ),
                              ),
                              disableNavigationbar: true,
                              edit: false,
                            ),
                          ),
                        )
                      ]
                    : [],
                Text("Transformer Script"),
                Container(
                  height: 200,
                  child: SingleChildScrollView(
                    child: CodeEditor(
                      model: EditorModel(
                        files: [FileEditor(name: "codigo", language: "java", code: dbStream.dataScript)],
                        styleOptions: EditorModelStyleOptions(
                          fontSize: 13,
                        ),
                      ),
                      disableNavigationbar: true,
                      edit: false,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    mainAxisSize: MainAxisSize.max,
                    children: <Widget>[
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.pause_circle_filled, color: Colors.yellow[800]), Text("${dbStream.waiting.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListNotProcessedMessages(topicMessages: dbStream.waiting),
                                  ));
                        },
                      ),
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.run_circle, color: Colors.blue), Text("${dbStream.processing.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListNotProcessedMessages(topicMessages: dbStream.processing),
                                  ));
                        },
                      ),
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.error, color: Colors.red), Text("${dbStream.error.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListNotProcessedMessages(topicMessages: dbStream.error),
                                  ));
                        },
                      ),
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.verified, color: Colors.green), Text("${dbStream.success.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListProcessedMessages(agenteUid: dbStream.uuid),
                                  ));
                        },
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisSize: MainAxisSize.max,
                    children: <Widget>[
                      Text("FROM: ${dbStream.from}"),
                      Icon(Icons.arrow_forward),
                      (dbStream.agentType == "C")
                          ? Column(
                              children: [
                                Text("TO (in case of true): ${dbStream.to}"),
                                Text("TO (in case of false): ${dbStream.to2}"),
                              ],
                            )
                          : Text("TO: ${dbStream.to}")
                    ],
                  ),
                ),
              ],
            ),
          );
        });
  }
}
