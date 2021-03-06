import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:frontend/service/hud_service.dart';
import 'package:frontend/service/service_locator.dart';
import 'package:frontend/store/hud_store.dart';
import 'package:modal_progress_hud/modal_progress_hud.dart';

class ParentWidget extends StatefulWidget {
  ParentWidget(
    this.child, {
    Key key,
  }) : super(key: key);

  final Widget child;

  @override
  State createState() => new ParentWidgetState();
}

class ParentWidgetState extends State<ParentWidget> {
  static ParentWidget of(BuildContext context) => context.findAncestorWidgetOfExactType<ParentWidget>();

  HudStore _store = Services.get<HudService>(HudService).store();

  @override
  Widget build(BuildContext context) {
    return Observer(
        builder: (ctx) => ModalProgressHUD(
              child: widget.child,
              inAsyncCall: _store.loading,
              opacity: 0.5,
              progressIndicator: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  CircularProgressIndicator(),
                  SizedBox(height: 10),
                  Text(
                    _store.text,
                    style: Theme.of(context).primaryTextTheme.subtitle2,
                  )
                ],
              ),
            ));
  }
}
