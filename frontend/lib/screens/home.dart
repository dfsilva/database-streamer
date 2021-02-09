import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:frontend/service/db_stream_service.dart';
import 'package:frontend/service/service_locator.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/screen/add_stream.dart';
import 'package:nats_message_processor_client/screen/agents_graph.dart';
import 'package:nats_message_processor_client/screen/agents_list.dart';
import 'package:nats_message_processor_client/service/db_stream_service.dart';
import 'package:nats_message_processor_client/service/service_locator.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  DbStreamService _processorService = Services.get<DbStreamService>(DbStreamService);

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text("Table Streams")),
        body: Observer(
          builder: (_) {
            // List<Agent> agents = _processorService.store().agents.values.toList();
            // return PageView(
            //   controller: _pageController,
            //   children: [AgentsList(agents: agents), GraphAgents(agents: agents)],
            // );
          },
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            Navigator.of(context).push(MaterialPageRoute(builder: (_) => AddAgentScreen()));
          },
          child: Icon(Icons.add),
        ),
      );
}
