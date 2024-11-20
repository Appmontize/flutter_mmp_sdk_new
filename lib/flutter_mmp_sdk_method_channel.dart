import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_mmp_sdk_platform_interface.dart';

/// An implementation of [FlutterMmpSdkPlatform] that uses method channels.
class MethodChannelFlutterMmpSdk extends FlutterMmpSdkPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_mmp_sdk');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
