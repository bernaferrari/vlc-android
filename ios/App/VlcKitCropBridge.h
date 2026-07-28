#import <Foundation/Foundation.h>
#import <VLCKit/VLCMediaPlayer.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Small Objective-C island for the VLCKit 4 crop selector.
 *
 * VLCKit exposes this selector to Objective-C but Swift's importer omits it
 * because of its legacy C-shaped signature. Keeping that interop detail here
 * lets the shared KMP crop control retain full iOS decoder support.
 */
@interface VlcKitCropBridge : NSObject

+ (void)applyCropNumerator:(uint32_t)numerator
                denominator:(uint32_t)denominator
                   toPlayer:(VLCMediaPlayer *)player
NS_SWIFT_NAME(VlcKitCropBridge.apply(_:denominator:to:));

@end

NS_ASSUME_NONNULL_END
