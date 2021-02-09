// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'db_stream_store.dart';

// **************************************************************************
// StoreGenerator
// **************************************************************************

// ignore_for_file: non_constant_identifier_names, unnecessary_brace_in_string_interps, unnecessary_lambdas, prefer_expression_function_bodies, lines_longer_than_80_chars, avoid_as, avoid_annotating_with_dynamic

mixin _$DbStreamStore on _DbStreamStore, Store {
  final _$dbStreamsAtom = Atom(name: '_DbStreamStore.dbStreams');

  @override
  ObservableMap<String, DbStream> get dbStreams {
    _$dbStreamsAtom.reportRead();
    return super.dbStreams;
  }

  @override
  set dbStreams(ObservableMap<String, DbStream> value) {
    _$dbStreamsAtom.reportWrite(value, super.dbStreams, () {
      super.dbStreams = value;
    });
  }

  final _$_DbStreamStoreActionController = ActionController(name: '_DbStreamStore');

  @override
  dynamic setAgents(List<DbStream> dbStreams) {
    final _$actionInfo = _$_DbStreamStoreActionController.startAction(name: '_DbStreamStore.setAgents');
    try {
      return super.setAgents(dbStreams);
    } finally {
      _$_DbStreamStoreActionController.endAction(_$actionInfo);
    }
  }

  @override
  dynamic setAgent(DbStream dbStream) {
    final _$actionInfo = _$_DbStreamStoreActionController.startAction(name: '_DbStreamStore.setAgent');
    try {
      return super.setAgent(dbStream);
    } finally {
      _$_DbStreamStoreActionController.endAction(_$actionInfo);
    }
  }

  @override
  String toString() {
    return '''
dbStreams: ${dbStreams}
    ''';
  }
}
