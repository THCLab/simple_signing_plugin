import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:simple_signing_plugin/simple_signing_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  dynamic _signature ='';
  String isVerified = '';
  late bool _verification;
  late bool isDeviceSecure;
  TextEditingController signController = TextEditingController();

  @override
  void initState() {
    super.initState();
    checkDeviceSecure();
  }

  void checkDeviceSecure() async{
    isDeviceSecure = await SimpleSigningPlugin.checkIfDeviceSecure();
  }


  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Signing example app'),
        ),
        body: Center(
          child: Column(
            children: [
              TextFormField(
                controller: signController,
              ),
              RawMaterialButton(
                onPressed: () async{
                  if(isDeviceSecure){
                    _signature = await SimpleSigningPlugin.signData(signController.text);
                    setState(() {});
                  }
                },
                child: const Text(
                  'Sign'
                ),
              ),
              Text(
               _signature.toString()
              ),
              RawMaterialButton(
                onPressed: () async{
                  if(isDeviceSecure && _signature.isNotEmpty){
                    _verification = await SimpleSigningPlugin.verifyData(_signature);
                    setState(() {
                      if(_verification == false){
                        isVerified = 'Invalid!';
                      }else{
                        isVerified = 'Valid!';
                      }
                    });
                  }
                },
                child: const Text(
                    'Verify'
                ),
              ),
              Text(
                  isVerified
              ),
            ],
          ),
        )
      ),
    );
  }
}
