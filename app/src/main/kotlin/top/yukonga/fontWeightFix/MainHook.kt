package top.yukonga.fontWeightFix

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag("XiaomiFontWeightFix")
        when (lpparam.packageName) {
            "com.android.systemui" -> {
                try {
                    val miuiConfigsClass = loadClassOrNull("com.miui.utils.configs.MiuiConfigs")
                    val mobileTypeDrawableClass = loadClassOrNull("com.android.systemui.statusbar.views.MobileTypeDrawable")
                    val miuiNotificationHeaderViewClass = loadClassOrNull("com.android.systemui.qs.MiuiNotificationHeaderView")

                    val miFontPath = getSystemProperties("ro.miui.ui.font.mi_font_path", "/system/fonts/MiSansVF.ttf")
                    val miFontTypeface = Typeface.Builder(miFontPath).setFontVariationSettings("'wght' 500").build()

                    XposedHelpers.setStaticObjectField(miuiConfigsClass, "sMiproTypeface", miFontTypeface)

                    miuiNotificationHeaderViewClass?.methodFinder()?.filter { name.startsWith("updateResources") }?.first()?.createAfterHook {
                        XposedBridge.log("updateResources Hooked")
                        it.thisObject.objectHelper().setObject("usingMiPro", true)
                    }

                    miuiConfigsClass?.methodFinder()?.filterByName("setMiuiStatusBarTypeface")?.first()?.createBeforeHook {
                        @Suppress("UNCHECKED_CAST")
                        val textView = it.args[0] as Array<TextView>
                        XposedBridge.log("setMiuiStatusBarTypeface1 Hooked")
                        val typeface = miFontTypeface
                        textView.forEach { tv ->
                            tv.typeface = typeface
                        }
                        it.result = null
                    }

                    mobileTypeDrawableClass?.methodFinder()?.filterByName("setMiuiStatusBarTypeface")?.first()?.createBeforeHook {
                        @Suppress("UNCHECKED_CAST")
                        val paint = it.args[0] as Array<Paint>
                        XposedBridge.log("setMiuiStatusBarTypeface2 Hooked")
                        val typeface = Typeface.Builder(miFontPath).setFontVariationSettings("'wght' 700").build()
                        paint.forEach { p ->
                            p.typeface = typeface
                        }
                        it.result = null
                    }
                } catch (t: Throwable) {
                    Log.ex(t)
                }
            }

            else -> return
        }
    }
}

@SuppressLint("PrivateApi")
fun getSystemProperties(key: String, default: String): String {
    val ret: String = try {
        Class.forName("android.os.SystemProperties").getDeclaredMethod("get", String::class.java).invoke(null, key) as String
    } catch (iAE: IllegalArgumentException) {
        throw iAE
    } catch (_: Exception) {
        default
    }
    return ret
}