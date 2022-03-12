import 'dart:async';

import 'package:flutter/services.dart';

///Main class with methods to sign and verify data and to check if the device is secured with a screen lock.
class SimpleSigningPlugin {
  ///Dart MethodChannel to connect native code necessary for keys and authentication to Dart platform.
  static const MethodChannel _channel = MethodChannel('simple_signing_plugin');

  ///Signs the provided string using natively generated RSA key.
  ///In order to work correctly, a secure screen lock has to be set up (this can be checked with checkIfDeviceSecure()).
  ///Returns signed data as "signature:data" if signing succeeds or false. An asynchronous function, has to be awaited.
  static Future<dynamic> signData(String data) async {
    var result = await _channel.invokeMethod('signData', {'data': data});
    if (result != false) {
      return result;
    } else {
      return false;
    }
  }

  ///Verifies provided data with local certificate. Returns true if the signature is valid and false if it's not.
  ///An asynchronous function, has to be awaited.
  static Future<bool> verifyData(String data) async {
    var result = await _channel.invokeMethod('verifyData', {'data': data});
    if (result == true) {
      return true;
    } else {
      return false;
    }
  }

  ///Checks if the screen lock has been set on the device. Returns true if it is set and false if not. An asynchronous function, has to be awaited.
  static Future<bool> checkIfDeviceSecure() async {
    var result = await _channel.invokeMethod('checkIfDeviceSecure');
    if (result == true) {
      return true;
    } else {
      return false;
    }
  }
}
