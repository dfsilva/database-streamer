class DbStream {
  final String title;
  final String description;
  final String table;
  final String schema;
  final String topic;
  final bool delete;
  final bool insert;
  final bool update;

  DbStream({this.title, this.description, this.table, this.schema, this.topic, this.delete, this.insert, this.update});

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
}
