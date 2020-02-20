package org.wordpress.mobile.ReactNativeGutenbergBridge;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergBridgeJS2Parent.MediaCallback;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergBridgeJS2Parent.MediaType;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergBridgeJS2Parent.OtherMediaOptionsReceivedCallback;
import org.wordpress.mobile.ReactNativeGutenbergBridge.GutenbergBridgeJS2Parent.RNMedia;
import org.wordpress.mobile.WPAndroidGlue.MediaOption;

import java.util.ArrayList;
import java.util.List;

public class RNReactNativeGutenbergBridgeModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext mReactContext;
    private final GutenbergBridgeJS2Parent mGutenbergBridgeJS2Parent;

    private static final String EVENT_NAME_REQUEST_GET_HTML = "requestGetHtml";
    private static final String EVENT_NAME_UPDATE_HTML = "updateHtml";
    private static final String EVENT_NAME_UPDATE_TITLE = "setTitle";
    private static final String EVENT_NAME_FOCUS_TITLE = "setFocusOnTitle";
    private static final String EVENT_NAME_MEDIA_UPLOAD = "mediaUpload";
    private static final String EVENT_NAME_MEDIA_APPEND = "mediaAppend";

    private static final String MAP_KEY_UPDATE_HTML = "html";
    private static final String MAP_KEY_UPDATE_TITLE = "title";
    private static final String MAP_KEY_MEDIA_FILE_UPLOAD_STATE = "state";
    private static final String MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_ID = "mediaId";
    private static final String MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_URL = "mediaUrl";
    private static final String MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_TYPE = "mediaType";
    private static final String MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_PROGRESS = "progress";
    private static final String MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_SERVER_ID = "mediaServerId";

    private static final int MEDIA_UPLOAD_STATE_UPLOADING = 1;
    private static final int MEDIA_UPLOAD_STATE_SUCCEEDED = 2;
    private static final int MEDIA_UPLOAD_STATE_FAILED = 3;
    private static final int MEDIA_UPLOAD_STATE_RESET = 4;

    private static final int MEDIA_SERVER_ID_UNKNOWN = 0;

    private static final String MEDIA_SOURCE_MEDIA_LIBRARY = "SITE_MEDIA_LIBRARY";
    private static final String MEDIA_SOURCE_DEVICE_LIBRARY = "DEVICE_MEDIA_LIBRARY";
    private static final String MEDIA_SOURCE_DEVICE_CAMERA = "DEVICE_CAMERA";
    private static final String MEDIA_SOURCE_MEDIA_EDITOR = "MEDIA_EDITOR";


    public RNReactNativeGutenbergBridgeModule(ReactApplicationContext reactContext,
            GutenbergBridgeJS2Parent gutenbergBridgeJS2Parent) {
        super(reactContext);
        mReactContext = reactContext;
        mGutenbergBridgeJS2Parent = gutenbergBridgeJS2Parent;
    }

    @Override
    public String getName() {
        return "RNReactNativeGutenbergBridge";
    }

    private void emitToJS(String eventName, @Nullable WritableMap data) {
        mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, data);
    }

    public void getHtmlFromJS() {
        emitToJS(EVENT_NAME_REQUEST_GET_HTML, null);
    }

    public void setHtmlInJS(String html) {
        WritableMap writableMap = new WritableNativeMap();
        writableMap.putString(MAP_KEY_UPDATE_HTML, html);
        emitToJS(EVENT_NAME_UPDATE_HTML, writableMap);
    }

    public void setTitleInJS(String title) {
        WritableMap writableMap = new WritableNativeMap();
        writableMap.putString(MAP_KEY_UPDATE_TITLE, title);
        emitToJS(EVENT_NAME_UPDATE_TITLE, writableMap);
    }

    public void setFocusOnTitleInJS() {
        WritableMap writableMap = new WritableNativeMap();
        emitToJS(EVENT_NAME_FOCUS_TITLE, writableMap);
    }

    public void appendNewMediaBlock(int mediaId, String mediaUri, String mediaType) {
        WritableMap writableMap = new WritableNativeMap();
        writableMap.putString(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_TYPE, mediaType);
        writableMap.putString(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_URL, mediaUri);
        writableMap.putInt(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_ID, mediaId);
        emitToJS(EVENT_NAME_MEDIA_APPEND, writableMap);
    }

    @ReactMethod
    public void provideToNative_Html(String html, String title, boolean changed) {
        mGutenbergBridgeJS2Parent.responseHtml(title, html, changed);
    }

    @ReactMethod
    public void editorDidMount(ReadableArray unsupportedBlockNames) {
        mGutenbergBridgeJS2Parent.editorDidMount(unsupportedBlockNames);
    }

    @ReactMethod
    public void requestMediaPickFrom(String mediaSource, ReadableArray filter, Boolean allowMultipleSelection, final Callback onMediaSelected) {
        MediaType mediaType = getMediaTypeFromFilter(filter);
        if (mediaSource.equals(MEDIA_SOURCE_MEDIA_LIBRARY)) {
            mGutenbergBridgeJS2Parent.requestMediaPickFromMediaLibrary(getNewMediaCallback(allowMultipleSelection, onMediaSelected), allowMultipleSelection, mediaType);
        } else if (mediaSource.equals(MEDIA_SOURCE_DEVICE_LIBRARY)) {
            mGutenbergBridgeJS2Parent.requestMediaPickFromDeviceLibrary(getNewMediaCallback(allowMultipleSelection, onMediaSelected), allowMultipleSelection, mediaType);
        } else if (mediaSource.equals(MEDIA_SOURCE_DEVICE_CAMERA)) {
            mGutenbergBridgeJS2Parent.requestMediaPickerFromDeviceCamera(getNewMediaCallback(allowMultipleSelection, onMediaSelected), mediaType);
        } else {
            mGutenbergBridgeJS2Parent.requestMediaPickFrom(mediaSource, getNewMediaCallback(allowMultipleSelection, onMediaSelected), allowMultipleSelection);
        }
    }

    private MediaType getMediaTypeFromFilter(ReadableArray filter) {
        switch (filter.size()) {
            case 1:
                return MediaType.getEnum(filter.getString(0));
            case 2:
                MediaType filter0 = MediaType.getEnum(filter.getString(0));
                MediaType filter1 = MediaType.getEnum(filter.getString(1));

                if ((filter0.equals(MediaType.VIDEO) && filter1.equals(MediaType.IMAGE))
                    || (filter0.equals(MediaType.IMAGE) && filter1.equals(MediaType.VIDEO))) {
                    return MediaType.MEDIA;
                }
            default:
                return MediaType.OTHER;
        }
    }

    @ReactMethod
    public void requestMediaImport(String url, final Callback onUploadMediaSelected) {
        mGutenbergBridgeJS2Parent.requestMediaImport(url, getNewMediaCallback(false, onUploadMediaSelected));
    }

    @ReactMethod
    public void mediaUploadSync() {
        mGutenbergBridgeJS2Parent.mediaUploadSync(getNewMediaCallback(false,null));
    }

    @ReactMethod
    public void requestImageFailedRetryDialog(final int mediaId) {
        mGutenbergBridgeJS2Parent.requestImageFailedRetryDialog(-mediaId); // local id positive again
    }

    @ReactMethod
    public void requestImageUploadCancelDialog(final int mediaId) {
        mGutenbergBridgeJS2Parent.requestImageUploadCancelDialog(-mediaId); // local id positive again
    }

    @ReactMethod
    public void requestImageUploadCancel(final int mediaId) {
        mGutenbergBridgeJS2Parent.requestImageUploadCancel(-mediaId); // local id positive again
    }

    @ReactMethod
    public void requestImageFullscreenPreview(String mediaUrl) {
        mGutenbergBridgeJS2Parent.requestImageFullscreenPreview(mediaUrl);
    }

    @ReactMethod
    public void requestMediaEditor(String mediaUrl, final Callback onUploadMediaSelected) {
        mGutenbergBridgeJS2Parent.requestMediaEditor(getNewMediaCallback(false, onUploadMediaSelected), mediaUrl);
    }

    @ReactMethod
    public void editorDidEmitLog(String message, int logLevel) {
        mGutenbergBridgeJS2Parent.editorDidEmitLog(message, GutenbergBridgeJS2Parent.LogLevel.valueOf(logLevel));
    }

    @ReactMethod
    public void editorDidAutosave() {
        mGutenbergBridgeJS2Parent.editorDidAutosave();
    }

    @ReactMethod
    public void getOtherMediaOptions(ReadableArray filter, final Callback jsCallback) {
        OtherMediaOptionsReceivedCallback otherMediaOptionsReceivedCallback = getNewOtherMediaReceivedCallback(jsCallback);
        MediaType mediaType = getMediaTypeFromFilter(filter);
        mGutenbergBridgeJS2Parent.getOtherMediaPickerOptions(otherMediaOptionsReceivedCallback, mediaType);
    }

    @ReactMethod
    public void fetchRequest(String path, Promise promise) {
        mGutenbergBridgeJS2Parent.performRequest(path,
                promise::resolve,
                errorBundle -> {
                    WritableMap writableMap = Arguments.makeNativeMap(errorBundle);
                    if (writableMap.hasKey("code")) {
                        String code = String.valueOf(writableMap.getInt("code"));
                        promise.reject(code, new Error(), writableMap);
                    } else {
                        promise.reject(new Error(), writableMap);
                    }
                });
    }

    private OtherMediaOptionsReceivedCallback getNewOtherMediaReceivedCallback(final Callback jsCallback) {
        return new OtherMediaOptionsReceivedCallback() {
            @Override public void onOtherMediaOptionsReceived(ArrayList<MediaOption> mediaOptions) {
                WritableArray writableArray = new WritableNativeArray();
                for (MediaOption mediaOption : mediaOptions) {
                    writableArray.pushMap(mediaOption.toMap());
                }
                jsCallback.invoke(writableArray);
            }
        };
    }

    private MediaCallback getNewMediaCallback(final Boolean allowMultipleSelection, final Callback jsCallback) {
        return new MediaCallback() {
            @Override
            public void onMediaFileSelected(List<RNMedia> mediaList) {
                if (allowMultipleSelection) {
                    WritableArray writableArray = new WritableNativeArray();
                    for (RNMedia media : mediaList) {
                        writableArray.pushMap(media.toMap());
                    }
                    jsCallback.invoke(writableArray);
                } else if (!mediaList.isEmpty()) {
                    jsCallback.invoke(mediaList.get(0).toMap());
                } else {
                    // if we have no media (e.g. when a content provider throws an exception during file copy), invoke
                    // the js callback with no arguments
                    jsCallback.invoke();
                }
            }

            @Override public void onUploadMediaFileClear(int mediaId) {
                setMediaFileUploadDataInJS(MEDIA_UPLOAD_STATE_RESET, mediaId, null, 0);
            }

            @Override
            public void onMediaFileUploadProgress(int mediaId, float progress) {
                setMediaFileUploadDataInJS(MEDIA_UPLOAD_STATE_UPLOADING, mediaId, null, progress);
            }

            @Override
            public void onMediaFileUploadSucceeded(int mediaId, String mediaUrl, int mediaServerId) {
                setMediaFileUploadDataInJS(MEDIA_UPLOAD_STATE_SUCCEEDED, mediaId, mediaUrl, 1, mediaServerId);
            }

            @Override
            public void onMediaFileUploadFailed(int mediaId) {
                setMediaFileUploadDataInJS(MEDIA_UPLOAD_STATE_FAILED, mediaId, null, 0);
            }
        };
    }

    private void setMediaFileUploadDataInJS(int state, int mediaId, String mediaUrl, float progress) {
        setMediaFileUploadDataInJS(state, mediaId, mediaUrl, progress, MEDIA_SERVER_ID_UNKNOWN);
    }

    private void setMediaFileUploadDataInJS(int state, int mediaId, String mediaUrl, float progress, int mediaServerId) {
        WritableMap writableMap = new WritableNativeMap();
        writableMap.putInt(MAP_KEY_MEDIA_FILE_UPLOAD_STATE, state);
        writableMap.putInt(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_ID, -mediaId); // negate local media id here
        writableMap.putString(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_URL, mediaUrl);
        writableMap.putDouble(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_PROGRESS, progress);
        if (mediaServerId != MEDIA_SERVER_ID_UNKNOWN) {
            writableMap.putInt(MAP_KEY_MEDIA_FILE_UPLOAD_MEDIA_SERVER_ID, mediaServerId);
        }
        emitToJS(EVENT_NAME_MEDIA_UPLOAD, writableMap);
    }

    public void toggleEditorMode() {
        emitToJS("toggleHTMLMode", null);
    }
}
