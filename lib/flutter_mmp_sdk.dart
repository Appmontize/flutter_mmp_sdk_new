import 'dart:async';
import 'package:flutter/services.dart';

class FlutterMmpSdk {
  static const MethodChannel _channel = MethodChannel('flutter_mmp_sdk');

  /// Initialize the SDK
  static Future<void> initialize() async {
    await _channel.invokeMethod('initialize');
  }

  /// Track `registered_now` event
  static Future<void> registeredNow() async {
    await _channel.invokeMethod('trackEvent', {'eventType': 'registered_now'});
  }

  /// Track `subscribed` event
  static Future<void> subscribed() async {
    await _channel.invokeMethod('trackEvent', {'eventType': 'subscribed'});
  }

  /// Track `completed` event
  static Future<void> trial() async {
    await _channel.invokeMethod('trackEvent', {'eventType': 'trial'});
  }
}
