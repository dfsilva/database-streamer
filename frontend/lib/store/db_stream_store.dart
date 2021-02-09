import 'package:frontend/dto/db_stream.dart';
import 'package:mobx/mobx.dart';

part 'db_stream_store.g.dart';

class DbStreamStore = _DbStreamStore with _$DbStreamStore;

abstract class _DbStreamStore with Store {
  @observable
  ObservableMap<String, DbStream> dbStreams = Map<String, DbStream>().asObservable();

  @action
  setAgents(List<DbStream> dbStreams) {
    this.dbStreams = Map<String, DbStream>.fromIterable(dbStreams, key: (p) => p.topic, value: (p) => p).asObservable();
  }

  @action
  setAgent(DbStream dbStream) {
    this.dbStreams[dbStream.topic] = dbStream;
  }
}
