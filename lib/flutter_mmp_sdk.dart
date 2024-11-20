import 'dart:async';
import 'package:flutter/services.dart';

class FlutterMmpSdk {
  static const MethodChannel _channel = MethodChannel('flutter_mmp_sdk');

  /// Initialize the SDK
  static Future<void> initialize() async {
    await _channel.invokeMethod('initialize');
  }

  /// Track `registered_now` event
  static Future<void> registeredNow(String clickId, String tid) async {
    await _channel.invokeMethod('trackEvent', {
      'eventType': 'registered_now',
      'clickId': clickId,
      'tid': tid,
    });
  }

  /// Track `subscribed` event
  static Future<void> subscribed(String clickId, String tid) async {
    await _channel.invokeMethod('trackEvent', {
      'eventType': 'subscribed',
      'clickId': clickId,
      'tid': tid,
    });
  }

  /// Track `completed` event
  static Future<void> completed(String clickId, String tid) async {
    await _channel.invokeMethod('trackEvent', {
      'eventType': 'completed',
      'clickId': clickId,
      'tid': tid,
    });
  }
}
