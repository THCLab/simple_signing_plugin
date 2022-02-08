#import "SimpleSigningPlugin.h"
#if __has_include(<simple_signing_plugin/simple_signing_plugin-Swift.h>)
#import <simple_signing_plugin/simple_signing_plugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "simple_signing_plugin-Swift.h"
#endif

@implementation SimpleSigningPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSimpleSigningPlugin registerWithRegistrar:registrar];
}
@end
