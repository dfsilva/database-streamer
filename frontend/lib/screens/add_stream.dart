import 'package:code_editor/code_editor.dart';
import 'package:flutter/material.dart';
import 'package:frontend/dto/db_stream.dart';
import 'package:frontend/service/db_stream_service.dart';
import 'package:frontend/service/service_locator.dart';

class AddAgentScreen extends StatefulWidget {
  final DbStream dbStream;

  AddAgentScreen({Key key, this.dbStream}) : super(key: key);

  @override
  _AddAgentScreenState createState() => _AddAgentScreenState();
}

class _AddAgentScreenState extends State<AddAgentScreen> {
  DbStreamService _dbStreamService = Services.get<DbStreamService>(DbStreamService);

  final _formKey = GlobalKey<FormState>();
  DbStream _dbStream;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Stream"),
      ),
      body: Column(
        children: [
          Expanded(
            child: Form(
                key: _formKey,
                child: ListView(
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        autofocus: true,
                        initialValue: _dbStream.title,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Please provide a title";
                          }
                          return null;
                        },
                        onSaved: (value) {
                          this._dbStream = this._dbStream.copyWith(title: value);
                        },
                        decoration: InputDecoration(hintText: "title", labelText: "title"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.multiline,
                        maxLines: 5,
                        textInputAction: TextInputAction.newline,
                        initialValue: _dbStream.description,
                        onSaved: (value) {
                          this._dbStream = this._dbStream.copyWith(description: value);
                        },
                        decoration: InputDecoration(hintText: "description", labelText: "description"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: Row(
                        children: [
                          Switch(
                            value: this._dbStream.ordered,
                            onChanged: (value) {
                              setState(() {
                                this._dbStream = this._dbStream.copyWith(ordered: value);
                              });
                            },
                          ),
                          Text("ordered")
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: DropdownButtonFormField(
                          value: this._dbStream.agentType,
                          items: <DropdownMenuItem>[
                            DropdownMenuItem(value: "D", child: Text("Default")),
                            DropdownMenuItem(value: "C", child: Text("Conditional"))
                          ],
                          onChanged: (value) {
                            setState(() {
                              this._dbStream = this._dbStream.copyWith(agentType: value);
                            });
                          },
                          onSaved: (type) {
                            this._dbStream = this._dbStream.copyWith(agentType: type);
                          }),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        initialValue: _dbStream.from,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Please inform the from topic";
                          }
                          return null;
                        },
                        onSaved: (from) {
                          this._dbStream = this._dbStream.copyWith(from: from);
                        },
                        decoration: InputDecoration(hintText: "from topic", labelText: "from"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        autofocus: true,
                        initialValue: _dbStream.to,
                        onSaved: (to) {
                          this._dbStream = this._dbStream.copyWith(to: to);
                        },
                        decoration: InputDecoration(hintText: "destiny topic 0", labelText: "topic0"),
                      ),
                    ),
                    (_dbStream.agentType == "C")
                        ? Padding(
                            padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                            child: TextFormField(
                              keyboardType: TextInputType.text,
                              autofocus: true,
                              initialValue: _dbStream.to2,
                              onSaved: (to2) {
                                this._dbStream = this._dbStream.copyWith(to2: to2);
                              },
                              decoration: InputDecoration(hintText: "destiny topic 1", labelText: "topic1"),
                            ),
                          )
                        : SizedBox.shrink(),
                    (_dbStream.agentType == "C")
                        ? Padding(
                            padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                            child: CodeEditor(
                                model: EditorModel(
                                  files: [FileEditor(name: "Conditional Script", language: "java", code: _dbStream.ifscript)],
                                  styleOptions: new EditorModelStyleOptions(
                                    fontSize: 13,
                                  ),
                                ),
                                disableNavigationbar: false,
                                onSubmit: (String language, String value) {
                                  this._dbStream = this._dbStream.copyWith(ifscript: value);
                                }),
                          )
                        : SizedBox.shrink(),
                    Padding(
                        padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                        child: CodeEditor(
                            model: EditorModel(
                              files: [FileEditor(name: "Data Script", language: "java", code: _dbStream.dataScript)],
                              styleOptions: new EditorModelStyleOptions(
                                fontSize: 13,
                              ),
                            ),
                            disableNavigationbar: false,
                            onSubmit: (String language, String value) {
                              this._dbStream = this._dbStream.copyWith(dataScript: value);
                            })),
                  ],
                )),
          ),


          Padding(
            padding: const EdgeInsets.only(bottom: 20, top: 10),
            child: RaisedButton(
                child: Text("Save"),
                onPressed: () {
                  if (_formKey.currentState.validate()) {
                    _formKey.currentState.save();
                  }

                  // _processorService.addOrUpdate(this._dbStream).then((value) => Navigator.of(context).pop());
                }),
          )
        ],
      ),
    );
  }
}
