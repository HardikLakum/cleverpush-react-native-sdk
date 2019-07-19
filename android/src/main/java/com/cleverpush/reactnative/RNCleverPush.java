package com.cleverpush.reactnative;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;

import com.cleverpush.ChannelTag;
import com.cleverpush.CleverPush;
import com.cleverpush.CustomAttribute;
import com.cleverpush.Notification;
import com.cleverpush.Subscription;
import com.cleverpush.listener.SubscribedListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public class RNCleverPush extends ReactContextBaseJavaModule implements LifecycleEventListener {
    public static final String NOTIFICATION_OPENED_INTENT_FILTER = "CPNotificationOpened";

    private CleverPush cleverPush;
    private ReactApplicationContext mReactApplicationContext;
    private ReactContext mReactContext;
    private boolean cleverPushInitDone;
    private boolean registeredEvents = false;

    private Callback pendingGetAvailableTagsCallback;
    private Callback pendingGetAvailableAttributesCallback;
    private Callback pendingGetSubscriptionTagsCallback;
    private Callback pendingGetSubscriptionAttributesCallback;
    private Callback pendingHasSubscriptionTagCallback;
    private Callback pendingGetSubscriptionAttributeCallback;
    private Callback pendingIsSubscribedCallback;

    public RNCleverPush(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactApplicationContext = reactContext;
        mReactContext = reactContext;
        mReactContext.addLifecycleEventListener(this);
        initCleverPush();
    }

    private String channelIdFromManifest(ReactApplicationContext context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), context.getPackageManager().GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString("cleverpush_channel_id");
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private void initCleverPush() {
        if (!registeredEvents) {
            registeredEvents = true;
            registerNotificationsOpenedNotification();
        }

        String channelId = channelIdFromManifest(mReactApplicationContext);

        if (channelId != null && channelId.length() > 0) {
            try {
                init(channelId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEvent(String eventName, Object params) {
        mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private JSONObject jsonFromErrorMessageString(String errorMessage) throws JSONException {
        return new JSONObject().put("error", errorMessage);
    }

    @ReactMethod
    public void init(String channelId) throws Exception {
        Context context = getCurrentActivity();

        if (cleverPushInitDone) {
            Log.e("cleverpush", "Already initialized the CleverPush React-Native SDK");
            return;
        }

        cleverPushInitDone = true;

        if (context == null) {
            context = mReactApplicationContext.getApplicationContext();
        }

        this.cleverPush = CleverPush.getInstance(context);
        cleverPush.init(channelId, new NotificationOpenedHandler(mReactContext), new SubscribedListener() {
            @Override
            public void subscribed(String subscriptionId) {
                notifySubscribed(subscriptionId);
            }
        });
    }

    @ReactMethod
    public void getSubscriptionTags(final Callback callback) {
        if (pendingGetSubscriptionTagsCallback == null)
            pendingGetSubscriptionTagsCallback = callback;

        Set<String> tags = this.cleverPush.getSubscriptionTags();
        WritableArray writableArray = new WritableNativeArray();
        for (String tag : tags) {
            writableArray.pushString(tag);
        }

        if (pendingGetSubscriptionTagsCallback != null)
            pendingGetSubscriptionTagsCallback.invoke(writableArray);

        pendingGetSubscriptionTagsCallback = null;
    }

    @ReactMethod
    public void hasSubscriptionTag(String tagId, final Callback callback) {
        if (pendingHasSubscriptionTagCallback == null)
            pendingHasSubscriptionTagCallback = callback;

        boolean hasTag = this.cleverPush.hasSubscriptionTag(tagId);

        if (pendingHasSubscriptionTagCallback != null)
            pendingHasSubscriptionTagCallback.invoke(hasTag);

        pendingHasSubscriptionTagCallback = null;
    }

    @ReactMethod
    public void getSubscriptionAttributes(final Callback callback) {
        if (pendingGetSubscriptionAttributesCallback == null)
            pendingGetSubscriptionAttributesCallback = callback;

        Map<String, String> attributes = this.cleverPush.getSubscriptionAttributes();
        WritableMap writableMap = new WritableNativeMap();
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            writableMap.putString(attribute.getKey(), attribute.getValue());
        }

        if (pendingGetSubscriptionAttributesCallback != null)
            pendingGetSubscriptionAttributesCallback.invoke(writableMap);

        pendingGetSubscriptionAttributesCallback = null;
    }

    @ReactMethod
    public void getSubscriptionAttribute(String attributeId, final Callback callback) {
        if (pendingGetSubscriptionAttributeCallback == null)
            pendingGetSubscriptionAttributeCallback = callback;

        String value = this.cleverPush.getSubscriptionAttribute(attributeId);

        if (pendingGetSubscriptionAttributeCallback != null)
            pendingGetSubscriptionAttributeCallback.invoke(value);

        pendingGetSubscriptionAttributeCallback = null;
    }

    @ReactMethod
    public void getAvailableTags(final Callback callback) {
        if (pendingGetAvailableTagsCallback == null)
            pendingGetAvailableTagsCallback = callback;

        Set<ChannelTag> tags = this.cleverPush.getAvailableTags();
        WritableArray writableArray = new WritableNativeArray();
        for (ChannelTag tag : tags) {
            WritableMap writeableMapTag = new WritableNativeMap();
            writeableMapTag.putString("id", tag.getId());
            writeableMapTag.putString("name", tag.getName());
            writableArray.pushMap(writeableMapTag);
        }

        if (pendingGetAvailableTagsCallback != null)
            pendingGetAvailableTagsCallback.invoke(writableArray);

        pendingGetAvailableTagsCallback = null;
    }

    @ReactMethod
    public void getAvailableAttributes(final Callback callback) {
        if (pendingGetAvailableAttributesCallback == null)
            pendingGetAvailableAttributesCallback = callback;

        Set<CustomAttribute> attributes = this.cleverPush.getAvailableAttributes();
        WritableArray writableArray = new WritableNativeArray();
        for (CustomAttribute attribute : attributes) {
            WritableMap writeableMapTag = new WritableNativeMap();
            writeableMapTag.putString("id", attribute.getId());
            writeableMapTag.putString("name", attribute.getName());
            writableArray.pushMap(writeableMapTag);
        }

        if (pendingGetAvailableAttributesCallback != null)
            pendingGetAvailableAttributesCallback.invoke(writableArray);

        pendingGetAvailableAttributesCallback = null;
    }

    @ReactMethod
    public void addSubscriptionTag(String tagId) {
        this.cleverPush.addSubscriptionTag(tagId);
    }

    @ReactMethod
    public void removeSubscriptionTag(String tagId) {
        this.cleverPush.removeSubscriptionTag(tagId);
    }

    @ReactMethod
    public void removeSubscriptionTag(String attributeId, String value) {
        this.cleverPush.setSubscriptionAttribute(attributeId, value);
    }

    @ReactMethod
    public void isSubscribed(final Callback callback) {
        if (pendingIsSubscribedCallback == null)
            pendingIsSubscribedCallback = callback;

        boolean isSubscribed = this.cleverPush.isSubscribed();

        if (pendingGetAvailableAttributesCallback != null)
            pendingGetAvailableAttributesCallback.invoke(isSubscribed);

        pendingGetAvailableAttributesCallback = null;
    }

    @ReactMethod
    public void setSubscriptionLanguage(String language) {
        this.cleverPush.setSubscriptionLanguage(language);
    }

    @ReactMethod
    public void setSubscriptionCountry(String country) {
        this.cleverPush.setSubscriptionCountry(country);
    }

    @ReactMethod
    public void subscribe() {
        this.cleverPush.subscribe();
    }

    @ReactMethod
    public void unsubscribe() {
        this.cleverPush.unsubscribe();
    }

    @ReactMethod
    public void showTopicsDialog() {
        this.cleverPush.showTopicsDialog();
    }

    @ReactMethod
    public Set<Notification> getNotifications() {
        return this.cleverPush.getNotifications();
    }

    private void registerNotificationsOpenedNotification() {
        IntentFilter intentFilter = new IntentFilter(NOTIFICATION_OPENED_INTENT_FILTER);
        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyNotificationOpened(intent.getExtras());
            }
        }, intentFilter);
    }

    private void notifySubscribed(String subscriptionId) {
        try {
            WritableMap result = new WritableNativeMap();
            result.putString("id", subscriptionId);
            sendEvent("CleverPush-subscribed", result);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void notifyNotificationOpened(Bundle bundle) {
        try {
            WritableMap result = new WritableNativeMap();

            Notification notification = (Notification) bundle.getSerializable("notification");
            if (notification != null) {
                WritableMap notificationMap = new WritableNativeMap();
                notificationMap.putString("id", notification.getId());
                notificationMap.putString("title", notification.getTitle());
                notificationMap.putString("text", notification.getText());
                notificationMap.putString("url", notification.getUrl());
                notificationMap.putString("iconUrl", notification.getIconUrl());
                notificationMap.putString("mediaUrl", notification.getMediaUrl());
                result.putMap("notification", notificationMap);
            }

            Subscription subscription = (Subscription) bundle.getSerializable("subscription");
            if (subscription != null) {
                WritableMap subscriptionMap = new WritableNativeMap();
                subscriptionMap.putString("id", subscription.getId());
                result.putMap("subscription", subscriptionMap);
            }

            sendEvent("CleverPush-notificationOpened", result);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "CleverPush";
    }

    @Override
    public void onHostDestroy() {
        this.cleverPush.removeNotificationOpenedListener();
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostResume() {
        initCleverPush();
    }
}
