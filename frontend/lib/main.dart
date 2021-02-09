import 'package:bot_toast/bot_toast.dart';
import 'package:flutter/material.dart';
import 'package:frontend/bus/rx_bus.dart';
import 'package:frontend/parent.dart';
import 'package:frontend/routes.dart';
import 'package:frontend/screens/home.dart';
import 'package:frontend/service/db_stream_service.dart';
import 'package:frontend/service/hud_service.dart';
import 'package:frontend/service/service_locator.dart';
import 'package:frontend/utils/navigator.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  final _rxBus = new RxBus();
  Services.add(HudService(_rxBus));
  Services.add(DbStreamService(_rxBus));

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Database Streamer',
        debugShowCheckedModeBanner: false,
        navigatorObservers: [BotToastNavigatorObserver()],
        navigatorKey: NavigatorUtils.nav,
        theme: ThemeData(
          brightness: Brightness.light,
        ),
        darkTheme: ThemeData(
          brightness: Brightness.dark,
        ),
        themeMode: ThemeMode.dark,
        routes: {Routes.HOME: (context) => HomeScreen()},
        builder: (ctx, widget) => BotToastInit()(ctx, ParentWidget(widget)),
        initialRoute: Routes.HOME,
      );
}
