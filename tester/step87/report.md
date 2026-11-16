# Home doctor

TV: Smart TV Pro (TCL), Android 14 (SDK 34)

**Winner: none: no angle kept Home on UrTV**

## Angle A: the HOME role to UrTV (cmd role add-role-holder --flags 1)
- applied: yes
- result: failed: Home opened com.google.android.apps.tv.launcherx at 6 s; two minutes later Home opened com.google.android.youtube.tv; the role went back to com.google.android.apps.tv.launcherx within two minutes
- Home from YouTube: com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s
- after two minutes: role com.google.android.apps.tv.launcherx; Home opened com.google.android.youtube.tv
- safety switch armed after: no; add-on bound: no
- why (the log):
```
11-17 00:24:49.318 V/WindowManager(  653): Stopping ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: nowVisible=true animating=true finishing=false
11-17 00:24:49.322 V/WindowManager(  653): Stopping ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: nowVisible=true animating=true finishing=false
11-17 00:24:49.381 V/WindowManager(  653): Stopping ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: nowVisible=true animating=true finishing=false
11-17 00:24:49.457 V/WindowManager(  653): Stopping ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: nowVisible=true animating=true finishing=false
11-17 00:24:49.463 V/WindowManager(  653): Stopping ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: nowVisible=true animating=true finishing=false
11-17 00:24:49.675 V/WindowManager(  653): Stopping ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: nowVisible=true animating=false finishing=false
11-17 00:24:49.676 V/WindowManager(  653): Ready to stop: ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}
11-17 00:24:49.677 V/WindowManager(  653): Moving to STOPPING: ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680} (stop requested)
11-17 00:24:49.677 V/WindowManager(  653): State movement: ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680} from:PAUSED to:STOPPING reason:stopIfPossible
11-17 00:24:49.677 D/PERFDATA/HeMonitorManagerService(  653): onStopActivity, procName:com.google.android.apps.tv.launcherx
11-17 00:24:49.678 I/wm_stop_activity(  653): [0,161693904,com.google.android.apps.tv.launcherx/.home.HomeActivity]
11-17 00:24:49.757 I/wm_on_stop_called( 1159): [161693904,com.google.android.apps.tv.launcherx.home.HomeActivity,STOP_ACTIVITY_ITEM,34]
11-17 00:24:49.764 I/WindowManager(  653): Saving icicle of ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}: Bundle[EMPTY_PARCEL]
11-17 00:24:49.764 V/WindowManager(  653): Moving to STOPPED: ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680} (stop complete)
11-17 00:24:49.764 V/WindowManager(  653): State movement: ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680} from:STOPPING to:STOPPED reason:activityStopped
11-17 00:24:53.351 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:24:53.352 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:24:53.657 W/ProcessStats(  653): Tracking association SourceState{fa810ca com.google.android.apps.tv.launcherx:coreservices/10044 BFgs #2289} whose proc state 4 is better than process ProcessState{95ed28a com.android.tv.settings/1000 pkg=com.android.tv.settings (multi)} proc state 8 (177 skipped)
11-17 00:24:58.464 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:24:58.465 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:24:58.937 W/ziparchive( 6398): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:24:58.937 W/ziparchive( 6398): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:24:58.938 W/artd    ( 6398): Could not get dex checksums for location /data/app/~~m9glt2VYVteXDWorCZ-jWA==/com.google.android.apps.tv.launcherx-HmKLLgTZzqJSHnEVbNFTiw==/base.apk, fd=-1
11-17 00:25:03.586 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:25:03.587 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
```

## Angle B: the home activity to UrTV (cmd package set-home-activity), the role untouched
- applied: yes
- result: failed: Home opened com.google.android.apps.tv.launcherx at 6 s; two minutes later Home opened com.google.android.apps.tv.launcherx
- Home from YouTube: com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s
- after two minutes: role sa.urtv.home; Home opened com.google.android.apps.tv.launcherx
- safety switch armed after: no; add-on bound: no
- why (the log):
```
11-17 00:28:15.424 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:28:15.425 V/WindowManager(  653): Sent Transition #25 createdAt=11-17 00:28:15.201 via request=TransitionRequestInfo { type = OPEN, triggerTask = TaskInfo{userId=0 taskId=11680 displayId=0 isRunning=true baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] flg=0x10800100 cmp=com.google.android.apps.tv.launcherx/.home.HomeActivity } baseActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} topActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} origActivity=null realActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} numActivities=1 lastActiveTime=527986 supportsMultiWindow=false resizeMode=0 isResizeable=false minWidth=-1 minHeight=-1 defaultMinSize=220 token=WCT{RemoteToken{e488c3f Task{8912801 #11680 type=home A=10044:com.google.android.apps.tv.launcherx}}} topActivityType=2 pictureInPictureParams=null shouldDockBigOverlays=true launchIntoPipHostTaskId=-1 lastParentTaskIdBeforePip=-1 displayCutoutSafeInsets=null topActivityInfo=ActivityInfo{d9062d5 com.google.android.apps.tv.launcherx.home.HomeActivity} launchCookies=[] positionInParent=Point(0, 0) parentTaskId=-1 isFocused=true isVisible=false isVisibleRequested=true isSleeping=false topActivityInSizeCompat=false topActivityEligibleForLetterboxEducation= false topActivityLetterboxed= false isFromDoubleTap= false topActivityLetterboxVerticalPosition= -1 topActivityLetterboxHorizontalPosition= -1 topActivityLetterboxWidth=-1 topActivityLetterboxHeight=-1 locusId=null displayAreaFeatureId=1 cameraCompatControlState=hidden}, remoteTransition = null, displayChange = null }
11-17 00:28:15.439 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:28:15.447 I/WindowManager(  653): window[ Window{ea44a2e u0 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} ] release screen wakelock.
11-17 00:28:15.447 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:28:15.454 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:28:15.460 W/ActivityTaskManager(  653): ActivityRecord idle....com.google.android.apps.tv.launcherx
11-17 00:28:15.461 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:28:15.471 D/SurfaceFlinger(  537): TFWD:Focus Visible window:com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity#247
11-17 00:28:15.473 D/InputDispatcher(  653): TFWD: Acquire New FocusedWindow: ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity OK
11-17 00:28:15.473 I/input_focus(  653): [Focus entering ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity (server),reason=Window became focusable. Previous reason: NOT_VISIBLE]
11-17 00:28:15.546 I/ImeTracker(  653): com.google.android.apps.tv.launcherx:9b4ccae4: onRequestHide at ORIGIN_SERVER_HIDE_INPUT reason HIDE_UNSPECIFIED_WINDOW
11-17 00:28:15.547 I/ImeTracker(  653): com.google.android.apps.tv.launcherx:9b4ccae4: onCancelled at PHASE_SERVER_SHOULD_HIDE
11-17 00:28:15.975 I/katniss_interactor_FindForegroundAppUtils( 3800): Foreground app name from running tasks: com.google.android.apps.tv.launcherx
11-17 00:28:15.975 I/katniss_interactor_UiClientOpHandler( 3800): Fetched foregroundAppPackage: com.google.android.apps.tv.launcherx
11-17 00:28:16.007 W/TrustedNetworkStatusMan( 2195): NetworkReauth: Fetching network count from TVSettings and Launcherx.
11-17 00:28:18.145 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:28:18.146 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:28:23.269 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:28:23.269 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:28:25.114 W/ziparchive( 7364): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:28:25.114 W/ziparchive( 7364): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:28:25.114 W/artd    ( 7364): Could not get dex checksums for location /data/app/~~m9glt2VYVteXDWorCZ-jWA==/com.google.android.apps.tv.launcherx-HmKLLgTZzqJSHnEVbNFTiw==/base.apk, fd=-1
11-17 00:28:28.385 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:28:28.385 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
```

## Angle C: launcherx's claim cleared (remove-role-holder, its preferred activities), then A
- applied: yes
- result: failed: Home opened com.google.android.apps.tv.launcherx at 6 s; two minutes later Home opened com.google.android.apps.tv.launcherx; the role went back to com.google.android.apps.tv.launcherx within two minutes
- Home from YouTube: com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s
- after two minutes: role com.google.android.apps.tv.launcherx; Home opened com.google.android.apps.tv.launcherx
- safety switch armed after: no; add-on bound: no
- why (the log):
```
11-17 00:31:39.255 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:31:39.259 I/sysui_multi_action(  653): [319,210,322,193,325,731,757,761,758,9,759,2,806,com.google.android.apps.tv.launcherx,871,com.google.android.apps.tv.launcherx.home.HomeActivity,905,0,1320,-1,1321,-1]
11-17 00:31:39.264 I/WindowManager(  653): window[ Window{ea44a2e u0 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} ] release screen wakelock.
11-17 00:31:39.269 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:31:39.269 D/Layer   (  537): TFWD: ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}#67 isHiddenByPolicy s.flags&layer_state_t::eLayerHidden=1
11-17 00:31:39.269 D/Layer   (  537): TFWD: isHiddenByPolicy parent ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680}#67 is hidden cause not visible
11-17 00:31:39.270 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:31:39.280 V/WindowManager(  653): Sent Transition #35 createdAt=11-17 00:31:39.064 via request=TransitionRequestInfo { type = OPEN, triggerTask = TaskInfo{userId=0 taskId=11680 displayId=0 isRunning=true baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] flg=0x10800100 cmp=com.google.android.apps.tv.launcherx/.home.HomeActivity } baseActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} topActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} origActivity=null realActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} numActivities=1 lastActiveTime=731849 supportsMultiWindow=false resizeMode=0 isResizeable=false minWidth=-1 minHeight=-1 defaultMinSize=220 token=WCT{RemoteToken{e488c3f Task{8912801 #11680 type=home A=10044:com.google.android.apps.tv.launcherx}}} topActivityType=2 pictureInPictureParams=null shouldDockBigOverlays=true launchIntoPipHostTaskId=-1 lastParentTaskIdBeforePip=-1 displayCutoutSafeInsets=null topActivityInfo=ActivityInfo{d9062d5 com.google.android.apps.tv.launcherx.home.HomeActivity} launchCookies=[] positionInParent=Point(0, 0) parentTaskId=-1 isFocused=true isVisible=false isVisibleRequested=true isSleeping=false topActivityInSizeCompat=false topActivityEligibleForLetterboxEducation= false topActivityLetterboxed= false isFromDoubleTap= false topActivityLetterboxVerticalPosition= -1 topActivityLetterboxHorizontalPosition= -1 topActivityLetterboxWidth=-1 topActivityLetterboxHeight=-1 locusId=null displayAreaFeatureId=1 cameraCompatControlState=hidden}, remoteTransition = null, displayChange = null }
11-17 00:31:39.287 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:31:39.288 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:31:39.336 D/SurfaceFlinger(  537): TFWD:Focus Visible window:com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity#311
11-17 00:31:39.337 D/InputDispatcher(  653): TFWD: Acquire New FocusedWindow: ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity OK
11-17 00:31:39.338 I/input_focus(  653): [Focus entering ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity (server),reason=Window became focusable. Previous reason: NOT_VISIBLE]
11-17 00:31:39.347 I/ImeTracker(  653): com.google.android.apps.tv.launcherx:5759b1e5: onRequestHide at ORIGIN_SERVER_HIDE_INPUT reason HIDE_UNSPECIFIED_WINDOW
11-17 00:31:39.347 I/ImeTracker(  653): com.google.android.apps.tv.launcherx:5759b1e5: onCancelled at PHASE_SERVER_SHOULD_HIDE
11-17 00:31:39.713 I/katniss_interactor_FindForegroundAppUtils( 3800): Foreground app name from running tasks: com.google.android.apps.tv.launcherx
11-17 00:31:39.713 I/katniss_interactor_UiClientOpHandler( 3800): Fetched foregroundAppPackage: com.google.android.apps.tv.launcherx
11-17 00:31:39.776 W/TrustedNetworkStatusMan( 2195): NetworkReauth: Fetching network count from TVSettings and Launcherx.
11-17 00:31:42.944 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:31:42.945 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:31:48.068 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:31:48.069 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:31:48.400 W/ziparchive( 8371): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:31:48.401 W/ziparchive( 8371): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:31:48.401 W/artd    ( 8371): Could not get dex checksums for location /data/app/~~m9glt2VYVteXDWorCZ-jWA==/com.google.android.apps.tv.launcherx-HmKLLgTZzqJSHnEVbNFTiw==/base.apk, fd=-1
```

## Angle D: A + «إضافات تلفزيونك»'s Home watch (on, bound, wanted)
- applied: yes
- result: failed: Home opened com.google.android.apps.tv.launcherx at 6 s; two minutes later Home opened com.google.android.apps.tv.launcherx
- Home from YouTube: sa.urtv.home at 2 s, sa.urtv.home at 6 s; sa.urtv.home at 2 s, sa.urtv.home at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s
- after two minutes: role sa.urtv.home; Home opened com.google.android.apps.tv.launcherx
- safety switch armed after: no; add-on bound: yes
- why (the log):
```
11-17 00:35:06.718 I/katniss_interactor_FindForegroundAppUtils( 3800): Foreground app name from running tasks: com.google.android.apps.tv.launcherx
11-17 00:35:06.718 I/katniss_interactor_UiClientOpHandler( 3800): Fetched foregroundAppPackage: com.google.android.apps.tv.launcherx
11-17 00:35:06.770 W/TrustedNetworkStatusMan( 2195): NetworkReauth: Fetching network count from TVSettings and Launcherx.
11-17 00:35:06.845 W/TrustedNetworkStatusMan( 2195): NetworkReauth: Fetching network count from TVSettings and Launcherx.
11-17 00:35:06.892 D/ConnectivityService(  653): requestNetwork for uid/pid:10044/2195 activeRequest: null callbackRequest: 257 [NetworkRequest [ REQUEST id=258, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]] callback flags: 0 order: 2147483647
11-17 00:35:06.895 D/WifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=258, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:06.898 D/UntrustedWifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=258, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:06.898 D/OemPaidWifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=258, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:06.898 D/MultiInternetWifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=258, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:07.174 D/ConnectivityService(  653): requestNetwork for uid/pid:10044/2195 activeRequest: null callbackRequest: 259 [NetworkRequest [ REQUEST id=260, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]] callback flags: 0 order: 2147483647
11-17 00:35:07.176 D/WifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=260, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:07.181 D/UntrustedWifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=260, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:07.181 D/OemPaidWifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=260, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:07.181 D/MultiInternetWifiNetworkFactory(  653): got request NetworkRequest [ REQUEST id=260, [ Capabilities: INTERNET&NOT_RESTRICTED&TRUSTED&NOT_VCN_MANAGED Uid: 10044 RequestorUid: 10044 RequestorPkg: com.google.android.apps.tv.launcherx UnderlyingNetworks: Null] ]
11-17 00:35:07.744 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:07.745 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:12.869 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:12.869 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:16.463 W/ziparchive( 9865): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:35:16.463 W/ziparchive( 9865): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:35:16.464 W/artd    ( 9865): Could not get dex checksums for location /data/app/~~m9glt2VYVteXDWorCZ-jWA==/com.google.android.apps.tv.launcherx-HmKLLgTZzqJSHnEVbNFTiw==/base.apk, fd=-1
11-17 00:35:17.985 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:17.985 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:23.105 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:35:23.105 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
```

## Angle E: the TV's own default-home screen, for the tester (manual)
- applied: yes
- result: failed: Home opened com.google.android.apps.tv.launcherx at 6 s; two minutes later Home opened com.google.android.apps.tv.launcherx
- Home from YouTube: com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s
- after two minutes: role sa.urtv.home; Home opened com.google.android.apps.tv.launcherx
- safety switch armed after: no; add-on bound: no
- why (the log):
```
11-17 00:39:48.675 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:39:48.676 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:39:48.680 D/WindowManager(  653): allResumedActivitiesIdle: rootTask=1 ActivityRecord{9a340d0 u0 com.google.android.apps.tv.launcherx/.home.HomeActivity t11680} not idle
11-17 00:39:48.689 I/sysui_multi_action(  653): [319,142,322,125,325,1221,757,761,758,9,759,2,806,com.google.android.apps.tv.launcherx,871,com.google.android.apps.tv.launcherx.home.HomeActivity,905,0,1320,-1,1321,-1]
11-17 00:39:48.692 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:39:48.693 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:39:48.696 W/ActivityTaskManager(  653): ActivityRecord idle....com.google.android.apps.tv.launcherx
11-17 00:39:48.698 V/WindowManager(  653): Sent Transition #59 createdAt=11-17 00:39:48.552 via request=TransitionRequestInfo { type = OPEN, triggerTask = TaskInfo{userId=0 taskId=11680 displayId=0 isRunning=true baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] flg=0x10800100 cmp=com.google.android.apps.tv.launcherx/.home.HomeActivity } baseActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} topActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} origActivity=null realActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} numActivities=1 lastActiveTime=1221330 supportsMultiWindow=false resizeMode=0 isResizeable=false minWidth=-1 minHeight=-1 defaultMinSize=220 token=WCT{RemoteToken{e488c3f Task{8912801 #11680 type=home A=10044:com.google.android.apps.tv.launcherx}}} topActivityType=2 pictureInPictureParams=null shouldDockBigOverlays=true launchIntoPipHostTaskId=-1 lastParentTaskIdBeforePip=-1 displayCutoutSafeInsets=null topActivityInfo=ActivityInfo{d9062d5 com.google.android.apps.tv.launcherx.home.HomeActivity} launchCookies=[] positionInParent=Point(0, 0) parentTaskId=-1 isFocused=true isVisible=false isVisibleRequested=true isSleeping=false topActivityInSizeCompat=false topActivityEligibleForLetterboxEducation= false topActivityLetterboxed= false isFromDoubleTap= false topActivityLetterboxVerticalPosition= -1 topActivityLetterboxHorizontalPosition= -1 topActivityLetterboxWidth=-1 topActivityLetterboxHeight=-1 locusId=null displayAreaFeatureId=1 cameraCompatControlState=hidden}, remoteTransition = null, displayChange = null }
11-17 00:39:48.708 D/SurfaceFlinger(  537): TFWD:Find Focus windowName=ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:39:48.710 D/InputDispatcher(  653): TFWD: InputWindows ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:39:48.741 D/SurfaceFlinger(  537): TFWD:Focus Visible window:com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity#475
11-17 00:39:48.742 D/InputDispatcher(  653): TFWD: Acquire New FocusedWindow: ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity OK
11-17 00:39:48.743 I/input_focus(  653): [Focus entering ea44a2e com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity (server),reason=Window became focusable. Previous reason: NOT_VISIBLE]
11-17 00:39:48.748 I/ImeTracker(  653): com.google.android.apps.tv.launcherx:ce6248f7: onRequestHide at ORIGIN_SERVER_HIDE_INPUT reason HIDE_UNSPECIFIED_WINDOW
11-17 00:39:48.748 I/ImeTracker(  653): com.google.android.apps.tv.launcherx:ce6248f7: onCancelled at PHASE_SERVER_SHOULD_HIDE
11-17 00:39:48.994 W/TrustedNetworkStatusMan( 2195): NetworkReauth: Fetching network count from TVSettings and Launcherx.
11-17 00:39:49.345 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:39:49.345 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:39:54.464 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:39:54.465 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:39:58.669 W/ziparchive(11284): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:39:58.669 W/ziparchive(11284): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:39:58.670 W/artd    (11284): Could not get dex checksums for location /data/app/~~m9glt2VYVteXDWorCZ-jWA==/com.google.android.apps.tv.launcherx-HmKLLgTZzqJSHnEVbNFTiw==/base.apk, fd=-1
11-17 00:39:59.584 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:39:59.585 I/mtk_and (  289): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
```

## Angle F: A + wanted, then a restart: the home re-applies the role at boot
- applied: yes
- result: failed: Home opened com.google.android.apps.tv.launcherx at 6 s; two minutes later Home opened com.google.android.apps.tv.launcherx; the role went back to com.google.android.apps.tv.launcherx within two minutes
- Home from YouTube: com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s; com.google.android.apps.tv.launcherx at 2 s, com.google.android.apps.tv.launcherx at 6 s
- after two minutes: role com.google.android.apps.tv.launcherx; Home opened com.google.android.apps.tv.launcherx
- safety switch armed after: no; add-on bound: no
- why (the log):
```
11-17 00:44:12.491 I/sysui_multi_action(  663): [319,300,322,281,325,224,757,761,758,9,759,2,806,com.google.android.apps.tv.launcherx,871,com.google.android.apps.tv.launcherx.home.HomeActivity,905,0,1320,-1,1321,-1]
11-17 00:44:12.494 D/SurfaceFlinger(  536): TFWD:Find Focus windowName=8fdc936 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:44:12.497 V/WindowManager(  663): Sent Transition #10 createdAt=11-17 00:44:12.214 via request=TransitionRequestInfo { type = OPEN, triggerTask = TaskInfo{userId=0 taskId=11685 displayId=0 isRunning=true baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] flg=0x10800100 cmp=com.google.android.apps.tv.launcherx/.home.HomeActivity } baseActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} topActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} origActivity=null realActivity=ComponentInfo{com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} numActivities=1 lastActiveTime=224042 supportsMultiWindow=false resizeMode=0 isResizeable=false minWidth=-1 minHeight=-1 defaultMinSize=220 token=WCT{RemoteToken{5c38db Task{b14901d #11685 type=home A=10044:com.google.android.apps.tv.launcherx}}} topActivityType=2 pictureInPictureParams=null shouldDockBigOverlays=true launchIntoPipHostTaskId=-1 lastParentTaskIdBeforePip=-1 displayCutoutSafeInsets=null topActivityInfo=ActivityInfo{28e3847 com.google.android.apps.tv.launcherx.home.HomeActivity} launchCookies=[] positionInParent=Point(0, 0) parentTaskId=-1 isFocused=true isVisible=false isVisibleRequested=true isSleeping=false topActivityInSizeCompat=false topActivityEligibleForLetterboxEducation= false topActivityLetterboxed= false isFromDoubleTap= false topActivityLetterboxVerticalPosition= -1 topActivityLetterboxHorizontalPosition= -1 topActivityLetterboxWidth=-1 topActivityLetterboxHeight=-1 locusId=null displayAreaFeatureId=1 cameraCompatControlState=hidden}, remoteTransition = null, displayChange = null }
11-17 00:44:12.505 D/InputDispatcher(  663): TFWD: InputWindows 8fdc936 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:44:12.506 I/WindowManager(  663): window[ Window{8fdc936 u0 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity} ] release screen wakelock.
11-17 00:44:12.512 D/SurfaceFlinger(  536): TFWD:Find Focus windowName=8fdc936 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity, notVisible=1
11-17 00:44:12.514 D/InputDispatcher(  663): TFWD: InputWindows 8fdc936 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity is NOT_VISIBLE
11-17 00:44:12.543 W/ActivityTaskManager(  663): ActivityRecord idle....com.google.android.apps.tv.launcherx
11-17 00:44:12.545 D/SurfaceFlinger(  536): TFWD:Focus Visible window:com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity#138
11-17 00:44:12.546 D/InputDispatcher(  663): TFWD: Acquire New FocusedWindow: 8fdc936 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity OK
11-17 00:44:12.546 I/input_focus(  663): [Focus entering 8fdc936 com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity (server),reason=Window became focusable. Previous reason: NOT_VISIBLE]
11-17 00:44:12.560 I/ImeTracker(  663): com.google.android.apps.tv.launcherx:cf87c94: onRequestHide at ORIGIN_SERVER_HIDE_INPUT reason HIDE_UNSPECIFIED_WINDOW
11-17 00:44:12.560 I/ImeTracker(  663): com.google.android.apps.tv.launcherx:cf87c94: onCancelled at PHASE_SERVER_SHOULD_HIDE
11-17 00:44:12.751 I/mtk_and (  290): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:44:12.752 I/mtk_and (  290): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:44:13.049 I/katniss_interactor_FindForegroundAppUtils( 3571): Foreground app name from running tasks: com.google.android.apps.tv.launcherx
11-17 00:44:13.049 I/katniss_interactor_UiClientOpHandler( 3571): Fetched foregroundAppPackage: com.google.android.apps.tv.launcherx
11-17 00:44:13.102 W/TrustedNetworkStatusMan( 2200): NetworkReauth: Fetching network count from TVSettings and Launcherx.
11-17 00:44:17.791 I/mtk_and (  290): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:44:17.792 I/mtk_and (  290): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:44:22.050 W/ziparchive( 6145): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:44:22.050 W/ziparchive( 6145): Unable to open '/data/user/0/com.google.android.apps.tv.launcherx/cache/1787936389793.dm': No such file or directory
11-17 00:44:22.051 W/artd    ( 6145): Could not get dex checksums for location /data/app/~~m9glt2VYVteXDWorCZ-jWA==/com.google.android.apps.tv.launcherx-HmKLLgTZzqJSHnEVbNFTiw==/base.apk, fd=-1
11-17 00:44:22.910 I/mtk_and (  290): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
11-17 00:44:22.911 I/mtk_and (  290): filter_and_renice 187 pname = com.google.android.apps.tv.launcherx
```

