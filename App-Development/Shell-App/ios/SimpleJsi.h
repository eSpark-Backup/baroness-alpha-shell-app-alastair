#ifdef __cplusplus
#import "react-native-simple-jsi.h"
#endif

#ifdef RCT_NEW_ARCH_ENABLED
#import "RNSimpleJsiSpec.h"

@interface SimpleJsi : NSObject <NativeSimpleJsiSpec>
#else
#import <React/RCTBridgeModule.h>

@interface SimpleJsi : NSObject <RCTBridgeModule>
#endif

@end
