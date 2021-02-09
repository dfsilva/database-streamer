import 'package:flutter/material.dart';
import 'package:frontend/dto/db_stream.dart';
import 'package:frontend/service/db_stream_service.dart';
import 'package:frontend/service/service_locator.dart';

class AddUpdateStream extends StatefulWidget {
  final DbStream dbStream;

  AddUpdateStream({Key key, this.dbStream = const DbStream()}) : super(key: key);

  @override
  _AddUpdateStreamState createState() => _AddUpdateStreamState();
}

class _AddUpdateStreamState extends State<AddUpdateStream> {
  DbStreamService _dbStreamService = Services.get<DbStreamService>(DbStreamService);

  final _formKey = GlobalKey<FormState>();
  DbStream _dbStream;

  @override
  void initState() {
    super.initState();
    _dbStream = widget.dbStream;
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
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        initialValue: _dbStream.table,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Please inform the table";
                          }
                          return null;
                        },
                        onSaved: (value) {
                          this._dbStream = this._dbStream.copyWith(table: value);
                        },
                        decoration: InputDecoration(hintText: "table", labelText: "table"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        initialValue: _dbStream.schema,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Please inform the schema";
                          }
                          return null;
                        },
                        onSaved: (value) {
                          this._dbStream = this._dbStream.copyWith(schema: value);
                        },
                        decoration: InputDecoration(hintText: "schema", labelText: "schema"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        initialValue: _dbStream.topic,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Please inform the topic";
                          }
                          return null;
                        },
                        onSaved: (value) {
                          this._dbStream = this._dbStream.copyWith(topic: value);
                        },
                        decoration: InputDecoration(hintText: "topic", labelText: "topic"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: Row(
                        children: [
                          Switch(
                            value: this._dbStream.insert,
                            onChanged: (value) {
                              setState(() {
                                this._dbStream = this._dbStream.copyWith(insert: value);
                              });
                            },
                          ),
                          Text("insert")
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: Row(
                        children: [
                          Switch(
                            value: this._dbStream.update,
                            onChanged: (value) {
                              setState(() {
                                this._dbStream = this._dbStream.copyWith(update: value);
                              });
                            },
                          ),
                          Text("update")
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: Row(
                        children: [
                          Switch(
                            value: this._dbStream.delete,
                            onChanged: (value) {
                              setState(() {
                                this._dbStream = this._dbStream.copyWith(delete: value);
                              });
                            },
                          ),
                          Text("delete")
                        ],
                      ),
                    ),
                  ],
                )),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 20, top: 10),
            child: ElevatedButton(
                child: Text("Save"),
                onPressed: () {
                  if (_formKey.currentState.validate()) {
                    _formKey.currentState.save();
                  }
                  if(widget.dbStream.topic != null){
                    _dbStreamService.update(this._dbStream).then((value) => Navigator.of(context).pop());
                  }else{
                    _dbStreamService.add(this._dbStream).then((value) => Navigator.of(context).pop());
                  }
                }),
          )
        ],
      ),
    );
  }
}
