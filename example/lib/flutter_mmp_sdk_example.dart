import 'package:flutter/material.dart';
import 'package:flutter_mmp_sdk/flutter_mmp_sdk.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize the SDK
  await FlutterMmpSdk.initialize();

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Flutter MMP SDK')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: () async {
                  await FlutterMmpSdk.registeredNow("sampleClickId", "sampleTid");
                },
                child: const Text("Registered Now"),
              ),
              ElevatedButton(
                onPressed: () async {
                  await FlutterMmpSdk.subscribed("sampleClickId", "sampleTid");
                },
                child: const Text("Subscribed"),
              ),
              ElevatedButton(
                onPressed: () async {
                  await FlutterMmpSdk.completed("sampleClickId", "sampleTid");
                },
                child: const Text("Completed"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
