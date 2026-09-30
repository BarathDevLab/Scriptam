package com.scriptam.app.bridge

import android.content.Context
import com.dokar.quickjs.QuickJs

/**
 * Root bridge entry-point exposed to JavaScript as the `Native` global object.
 * Installs all sub-modules into the QuickJS context.
 */
class AndroidBridge(
    private val context: Context,
    private val uiModule: UIModule,
    private val storageModule: StorageModule,
    private val networkModule: NetworkModule,
    private val clipboardModule: ClipboardModule = ClipboardModule(context),
    private val notificationModule: NotificationModule = NotificationModule(context),
    private val deviceInfoModule: DeviceInfoModule = DeviceInfoModule(context)
) {

    /**
     * Installs all native API modules into the given [QuickJs] context.
     * After calling this, JavaScript code can use:
     *
     * ```js
     * Native.showToast("Hello");
     * Native.alert("Title", "Message");
     * Native.storage.get("key");
     * Native.storage.set("key", "value");
     * Native.clipboard.copy("text");
     * Native.clipboard.paste();
     * Native.notify("Title", "Body");
     * Native.device();            // returns JSON string
     * var response = Native.fetch("https://api.example.com/data");
     * ```
     */
    suspend fun install(quickJs: QuickJs) {
        uiModule.install(quickJs)
        storageModule.install(quickJs)
        networkModule.install(quickJs)
        clipboardModule.install(quickJs)
        notificationModule.install(quickJs)
        deviceInfoModule.install(quickJs)

        // Wire everything under a `Native` namespace in JS
        quickJs.evaluate<Any?>(
            """
            var Native = {
                showToast:  __native_showToast,
                alert:      __native_alert,
                storage: {
                    get:    __native_storage_get,
                    set:    __native_storage_set,
                    remove: __native_storage_remove
                },
                clipboard: {
                    copy:   __native_clipboard_copy,
                    paste:  __native_clipboard_paste
                },
                fetch:      __native_fetch,
                notify:     __native_notify,
                device:     __native_device
            };
            """.trimIndent()
        )
    }
}
