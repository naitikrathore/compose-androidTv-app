#!/bin/bash

# HOW TO USE THIS SCRIPT:

# 1. Specify the destination path where files will be copied.
#    Update this path according to your local setup.

# Example:
#   If your livetvx repository is located at '/home/yourusername/Projects/GoogleReferenceApp/livetxv',
#   set the destination path as follows:
#   destPath=/home/yourusername/Projects/GoogleReferenceApp/livetvx

destPath=/c/Users/tirkajla/GoogleReferenceApp/livetvx

cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastZapBanner.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastZapBanner.kt
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastZapBannerDataProvider.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastZapBannerDataProvider.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastAudioSubtitleList.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastAudioSubtitleList.kt 
cp app/src/main/res/layout/fast_zap_banner.xml $destPath/google_reference/app/src/main/res/layout/fast_zap_banner.xml 
cp app/src/main/res/layout/fast_audio_subtitles_list_layout.xml $destPath/google_reference/app/src/main/res/layout/fast_audio_subtitles_list_layout.xml 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastHomeData.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastHomeData.kt 
cp app/src/main/res/layout/fast_home_data.xml $destPath/google_reference/app/src/main/res/layout/fast_home_data.xml 
cp app/src/main/java/com/iwedia/cltv/components/ReferenceWidgetForYou.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/ReferenceWidgetForYou.kt 
cp app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailItemViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailItemViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailItemAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailItemAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailsViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/scene/home_scene/rail/RailsViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTab.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTab.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabChannelListAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabChannelListAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabChannelListViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabChannelListViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabDataProvider.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabDataProvider.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventListAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventListAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventListViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventListViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventsContainerAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventsContainerAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventsContainerViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabEventsContainerViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabTimelineAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabTimelineAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabTimelineViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/FastLiveTabTimelineViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastCategoryAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastCategoryAdapter.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastCategoryItemViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastCategoryItemViewHolder.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastErrorInfo.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastErrorInfo.kt 
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/foryou/RailItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/foryou/RailItem.kt
cp app/src/main/res/layout/layout_fast_no_internet_info.xml $destPath/google_reference/app/src/main/res/layout/layout_fast_no_internet_info.xml 
cp app/src/main/res/layout/guide_events_container_item.xml $destPath/google_reference/app/src/main/res/layout/guide_events_container_item.xml 
cp app/src/main/res/layout/guide_channel_list_item_fast.xml $destPath/google_reference/app/src/main/res/layout/guide_channel_list_item_fast.xml 
cp app/src/main/res/layout/layout_widget_guide_scene_fast.xml $destPath/google_reference/app/src/main/res/layout/layout_widget_guide_scene_fast.xml 
cp app/src/main/res/layout/guide_event_list_item.xml $destPath/google_reference/app/src/main/res/layout/guide_event_list_item.xml 
cp app/src/main/res/layout/guide_timeline_item.xml $destPath/google_reference/app/src/main/res/layout/guide_timeline_item.xml 
cp app/src/main/res/layout/layout_fast_live_tab_loading_placeholder.xml $destPath/google_reference/app/src/main/res/layout/layout_fast_live_tab_loading_placeholder.xml 
cp app/src/main/java/com/iwedia/cltv/TimeTextView.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/TimeTextView.kt 
cp app/src/main/java/com/iwedia/cltv/components/CustomCard.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/CustomCard.kt 
cp app/src/main/res/layout/custom_card.xml $destPath/google_reference/app/src/main/res/layout/custom_card.xml 
cp app/src/main/java/com/iwedia/cltv/components/welcome_screen/CustomWelcomeScreen.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/welcome_screen/CustomWelcomeScreen.kt 
cp app/src/main/java/com/iwedia/cltv/components/welcome_screen/CustomSpannableTextView.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/welcome_screen/CustomSpannableTextView.kt 
cp app/src/main/java/com/iwedia/cltv/components/welcome_screen/ColorHelper.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/welcome_screen/ColorHelper.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/FastButton.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/FastButton.kt 
cp app/src/main/res/layout/layout_custom_welcome_screen.xml $destPath/google_reference/app/src/main/res/layout/layout_custom_welcome_screen.xml 
cp app/src/main/res/layout/layout_custom_item.xml $destPath/google_reference/app/src/main/res/layout/layout_custom_item.xml 
cp app/src/main/java/com/iwedia/cltv/config/ConfigColorManager.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/config/ConfigColorManager.kt 
cp app/src/main/java/com/iwedia/cltv/config/ConfigFontManager.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/config/ConfigFontManager.kt 
cp app/src/main/java/com/iwedia/cltv/config/ConfigStringsManager.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/config/ConfigStringsManager.kt 
cp app/src/main/java/com/iwedia/cltv/utils/AnimationListener.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/utils/AnimationListener.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/epg/AnimationHelper.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/epg/AnimationHelper.kt 
cp app/src/main/java/com/iwedia/cltv/utils/Utils.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/utils/Utils.kt 
cp app/src/main/java/com/iwedia/cltv/anoki_fast/Utils.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/anoki_fast/Utils.kt 
cp app/src/main/java/com/iwedia/cltv/components/CustomButton.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/CustomButton.kt 
cp app/src/main/res/layout/custom_button.xml $destPath/google_reference/app/src/main/res/layout/custom_button.xml 
cp app/src/main/res/values/attrs.xml $destPath/google_reference/app/src/main/res/values/attrs.xml 
cp app/src/main/java/com/iwedia/cltv/components/CustomDetails.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/CustomDetails.kt 
cp app/src/main/res/layout/custom_details.xml $destPath/google_reference/app/src/main/res/layout/custom_details.xml 
cp app/src/main/res/layout/custom_details_first_row.xml $destPath/google_reference/app/src/main/res/layout/custom_details_first_row.xml 
cp app/src/main/res/layout/custom_details_parental_rating.xml $destPath/google_reference/app/src/main/res/layout/custom_details_parental_rating.xml 
cp app/src/main/res/layout/custom_details_time.xml $destPath/google_reference/app/src/main/res/layout/custom_details_time.xml 
cp app/src/main/res/layout/custom_details_info_row.xml $destPath/google_reference/app/src/main/res/layout/custom_details_info_row.xml 
cp app/src/main/res/layout/custom_details_icons_row.xml $destPath/google_reference/app/src/main/res/layout/custom_details_icons_row.xml 
cp app/src/main/res/layout/custom_details_audio_tracks_row.xml $destPath/google_reference/app/src/main/res/layout/custom_details_audio_tracks_row.xml 
cp app/src/main/res/layout/subtitle_tracks_linear_layout.xml $destPath/google_reference/app/src/main/res/layout/subtitle_tracks_linear_layout.xml 
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/FastInfoItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/FastInfoItem.kt
cp app/src/main/java/com/iwedia/cltv/components/welcome_screen/CustomWelcomeScreenWebView.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/welcome_screen/CustomWelcomeScreenWebView.kt
cp app/src/main/res/layout/layout_web_view.xml $destPath/google_reference/app/src/main/res/layout/layout_web_view.xml
cp app/src/main/res/layout/custom_details_cc_tracks_row.xml $destPath/google_reference/app/src/main/res/layout/custom_details_cc_tracks_row.xml
cp app/src/main/res/layout/custom_details_audio_type_row.xml $destPath/google_reference/app/src/main/res/layout/custom_details_audio_type_row.xml
cp app/src/main/java/com/iwedia/cltv/components/FadeAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/FadeAdapter.kt
cp app/src/main/java/com/iwedia/cltv/components/CheckListAdapter.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/CheckListAdapter.kt
cp app/src/main/java/com/iwedia/cltv/components/HorizontalButtonsAdapterViewHolder.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/components/HorizontalButtonsAdapterViewHolder.kt
cp -r app/src/main/res/drawable* $destPath/google_reference/app/src/main/res/
#cp -r app/src/main/res/drawable-mdpi $destPath/google_reference/app/src/main/res/
#cp -r app/src/main/res/drawable-xhdpi $destPath/google_reference/app/src/main/res/
#cp -r app/src/main/res/drawable-xxhdpi $destPath/google_reference/app/src/main/res/
#cp -r app/src/main/res/drawable-xxxhdpi $destPath/google_reference/app/src/main/res/
cp sdk/src/main/res/values/strings.xml $destPath/google_reference/sdk/src/main/res/values/strings.xml
cp sdk/src/main/res/values-es/strings.xml $destPath/google_reference/sdk/src/main/res/values-es/strings.xml
cp sdk/src/main/res/values-fr/strings.xml $destPath/google_reference/sdk/src/main/res/values-fr/strings.xml
cp app/src/main/res/layout/loading_layout_rail_main.xml $destPath/google_reference/app/src/main/res/layout/loading_layout_rail_main.xml
cp app/src/main/res/layout/rail_item.xml $destPath/google_reference/app/src/main/res/layout/rail_item.xml
cp app/src/main/res/values/values.xml $destPath/google_reference/app/src/main/res/values/values.xml
cp app/base/common/src/main/java/com/iwedia/cltv/platform/base/content_provider/FastDataProvider.kt $destPath/google_reference/app/base/common/src/main/java/com/iwedia/cltv/platform/base/content_provider/FastDataProvider.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/FastFavoriteItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/FastFavoriteItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/FastRatingItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/FastRatingItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/FastRatingListItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/FastRatingListItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/FastTosOptInItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/FastTosOptInItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/FastUserSettingsItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/FastUserSettingsItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/PromotionItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/PromotionItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/RecommendationItem.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/RecommendationItem.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/RecommendationRow.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/RecommendationRow.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/AdvertisingIdHelper.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/AdvertisingIdHelper.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/ApiError.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/ApiError.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/ApiException.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/ApiException.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/CompressionInterceptor.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/CompressionInterceptor.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/ErrorInterceptor.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/ErrorInterceptor.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastAnokiUidHelper.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastAnokiUidHelper.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastBackendAPI.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastBackendAPI.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastRetrofitHelper.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastRetrofitHelper.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastTosOptInHelper.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastTosOptInHelper.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastUrlHelper.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/FastUrlHelper.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/IpAddressHelper.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/IpAddressHelper.kt
cp app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/KeyApiInterceptor.kt $destPath/google_reference/app/platform/src/main/java/com/iwedia/cltv/platform/model/fast_backend_utils/KeyApiInterceptor.kt
cp app/src/main/java/com/iwedia/cltv/tis/helper/ChannelListHelper.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/tis/helper/ChannelListHelper.kt
cp app/src/main/java/com/iwedia/cltv/tis/helper/ScanHelper.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/tis/helper/ScanHelper.kt
cp app/src/main/java/com/iwedia/cltv/tis/main/AnokiTvInputService.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/tis/main/AnokiTvInputService.kt
cp app/src/main/java/com/iwedia/cltv/tis/model/ChannelDescriptor.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/tis/model/ChannelDescriptor.kt
cp app/src/main/java/com/iwedia/cltv/tis/model/ProgramDescriptor.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/tis/model/ProgramDescriptor.kt
cp app/src/main/java/com/iwedia/cltv/tis/ui/SetupActivity.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/tis/ui/SetupActivity.kt
cp app/src/main/java/com/iwedia/cltv/scene/home_scene/HomeSceneFast.kt $destPath/google_reference/app/src/main/java/com/iwedia/cltv/scene/home_scene/HomeSceneFast.kt



