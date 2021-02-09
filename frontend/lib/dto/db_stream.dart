class DbStream {
  final String title;
  final String description;
  final String table;
  final String schema;
  final String topic;
  final bool delete;
  final bool insert;
  final bool update;

  const DbStream({this.title, this.description, this.table, this.schema, this.topic, this.delete = true, this.insert = true, this.update = true});

  static DbStream fromJson(Map<String, Object> json) {
    return DbStream(
        title: json["title"],
        description: json["description"],
        table: json["table"],
        schema: json["schema"],
        topic: json["topic"],
        delete: json["delete"],
        insert: json["insert"],
        update: json["update"]);
  }

  Map<String, Object> toJson() => {
        "title": this.title,
        "description": this.description,
        "table": this.table,
        "schema": this.schema,
        "topic": this.topic,
        "delete": this.delete,
        "insert": this.insert,
        "update": this.update
      };

  DbStream copyWith(
          {String title,
          String description,
          String table,
          String schema,
          String topic,
          bool delete,
          bool insert,
          bool update}) =>
      DbStream(
        title: title ?? this.title,
        description: description ?? this.description,
        table: table ?? this.table,
        schema: schema ?? this.schema,
        topic: topic ?? this.topic,
        delete: delete != null ? delete : this.delete,
        insert: insert != null ? insert : this.insert,
        update: update != null ? update : this.update,
      );
}
