import 'package:flutter/foundation.dart';
import 'package:frontend/dto/db_stream.dart';

@immutable
class ShowHud {
  final String text;
  ShowHud({this.text = "Carregando..."});
}

@immutable
class HideHud {}

@immutable
class SetDbStreams {
  final List<DbStream> streams;
  SetDbStreams(this.streams);
}

@immutable
class SetDbStream {
  final DbStream dbStream;
  SetDbStream(this.dbStream);
}
