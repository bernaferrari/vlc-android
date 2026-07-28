#import "VlcKitCropBridge.h"

@implementation VlcKitCropBridge

+ (void)applyCropNumerator:(uint32_t)numerator
                denominator:(uint32_t)denominator
                   toPlayer:(VLCMediaPlayer *)player
{
    [player setCropRatioWithNumerator:numerator denominator:denominator];
}

@end
