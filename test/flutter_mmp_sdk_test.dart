import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_mmp_sdk/flutter_mmp_sdk.dart';
import 'package:flutter_mmp_sdk/flutter_mmp_sdk_platform_interface.dart';
import 'package:flutter_mmp_sdk/flutter_mmp_sdk_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterMmpSdkPlatform
    with MockPlatformInterfaceMixin
    implements FlutterMmpSdkPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterMmpSdkPlatform initialPlatform = FlutterMmpSdkPlatform.instance;

  test('$MethodChannelFlutterMmpSdk is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterMmpSdk>());
  });

  test('getPlatformVersion', () async {
    FlutterMmpSdk flutterMmpSdkPlugin = FlutterMmpSdk();
    MockFlutterMmpSdkPlatform fakePlatform = MockFlutterMmpSdkPlatform();
    FlutterMmpSdkPlatform.instance = fakePlatform;

    expect(await flutterMmpSdkPlugin.getPlatformVersion(), '42');
  });
}
