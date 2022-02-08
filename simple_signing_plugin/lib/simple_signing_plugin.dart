
import 'dart:async';

import 'package:flutter/services.dart';

class SimpleSigningPlugin {
  static const MethodChannel _channel = MethodChannel('simple_signing_plugin');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<dynamic> signData(String data) async{
    try{
      var result = await _channel.invokeMethod('signData', {'data': data});
      if(result != false){
        return result;
      }else{
        return false;
      }
    }on PlatformException catch (e){
      return false;
    }
  }

  static Future<bool> verifyData(String data) async{
    var result = await _channel.invokeMethod('verifyData', {'data': data});
    if(result == true){
      return true;
    }else{
      return false;
    }
  }

  static Future<bool> checkIfDeviceSecure() async{
    var result = await _channel.invokeMethod('checkIfDeviceSecure');
    if(result == true){
      return true;
    }else{
      return false;
    }
  }

}
