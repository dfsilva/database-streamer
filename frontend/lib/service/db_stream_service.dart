import 'dart:async';

import 'package:frontend/bus/actions.dart';
import 'package:frontend/dto/db_stream.dart';
import 'package:frontend/service/base_service.dart';
import 'package:frontend/store/db_stream_store.dart';
import 'package:frontend/utils/http_utils.dart';

class DbStreamService extends BaseService<DbStreamStore> {
  DbStreamService(rxBus) : super(rxBus, DbStreamStore());

  Future<DbStream> add(DbStream dbStream) {
    bus().send(ShowHud(text: "Enviando..."));
    return Api.doPost(uri: "/streams", bodyParams: dbStream.toJson())
        .then((value) => DbStream.fromJson(value))
        .then((value) => bus().send(SetDbStream(value)).dbStream)
        .whenComplete(() => bus().send(HideHud()));
  }

  Future<DbStream> update(DbStream dbStream) {
    bus().send(ShowHud(text: "Enviando..."));
    return Api.doPut(uri: "/streams/${dbStream.topic}", bodyParams: dbStream.toJson())
        .then((value) => DbStream.fromJson(value))
        .then((value) => bus().send(SetDbStream(value)).dbStream)
        .whenComplete(() => bus().send(HideHud()));
  }

  Future<List<DbStream>> dbStreams() {
    return Api.doGet(uri: "/streams").then((value) => (value as Iterable).map((e) => DbStream.fromJson(e)).toList());
  }

  @override
  void dispose() {}

  @override
  void onReceiveMessage(msg) {
    if (msg is SetDbStreams) {
      store().setAgents(msg.streams);
    }
    if (msg is SetDbStream) {
      store().setAgent(msg.dbStream);
    }
  }
}
