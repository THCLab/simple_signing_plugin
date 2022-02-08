A plugin for signing and verifying strings. It uses an RSA key and user authentication via biometrics/pin is required.

## Getting Started
In order for the package to function properly, it is required to check if the device is protected with a screen lock.
It can be done using `checkIfDeviceSecure` :
```dart
isDeviceSecure = await checkIfDeviceSecure(); //returns true if screen lock is set
```
All plugin functions are blocked if there is no screen lock set.

## Usage
```dart
//Signing data example
late dynamic _signature;
String _dataToSign = 'Sign me!';
if(isDeviceSecure){
   _signature = await signData(_dataToSign); //returns signature or false if there is an error
}
```

```dart
//Verifying data example
late bool isValid;
String _dataToVerify = 'T3xg2+3NlJssuUG9Cd4OSZ9GXw6f+rZ6gOT2SmVvHSJTzQfzTioPJze+F13QvK4JuLa/rTljFd+KQvH1pPUmKPiEyKG9xAVlqa7LJjtetjYRYCliCn/dfRb1qWK6o/47zlWPbKR+lEOZyokKZNJmNyLyqpnKx2m1c9cG8bWf7jHSzC4GM/yXAKhQu+4UA6DMkdxOGaMhmYR26NMfnSxcfUn61eeZvz151/qG2GFCprOst8/ab/3el7T0AKn1X1eZ9TZBzLVgaMwVYRZg3JiF1MA3OHYc4rhJvYVTrXimzIcyYEJv7boNhf1b1p1c8qbRIrzKekiEpon5Wfi1IQzzQA==:k';
if(isDeviceSecure){
   isValid = await verifyData(_dataToVerify); //returns true or false
}
```