import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_mmp_sdk_method_channel.dart';

abstract class FlutterMmpSdkPlatform extends PlatformInterface {
  /// Constructs a FlutterMmpSdkPlatform.
  FlutterMmpSdkPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterMmpSdkPlatform _instance = MethodChannelFlutterMmpSdk();

  /// The default instance of [FlutterMmpSdkPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterMmpSdk].
  static FlutterMmpSdkPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterMmpSdkPlatform] when
  /// they register themselves.
  static set instance(FlutterMmpSdkPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
