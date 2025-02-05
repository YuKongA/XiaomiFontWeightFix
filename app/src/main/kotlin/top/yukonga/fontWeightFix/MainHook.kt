package top.yukonga.fontWeightFix

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

                    XposedHelpers.setStaticObjectField(miuiConfigsClass, "sMiproTypeface", miFontTypeface(430))
                    XposedHelpers.setStaticObjectField(mobileTypeDrawableClass, "sMiproTypeface", miFontTypeface(520))

                    miuiNotificationHeaderViewClass?.methodFinder()?.filter { name.startsWith("updateResources") }?.first()?.createAfterHook {
                        it.thisObject.objectHelper().setObject("usingMiPro", true)
                    }

                    miuiConfigsClass?.methodFinder()?.filterByName("setMiuiStatusBarTypeface")?.first()?.createBeforeHook {
                        @Suppress("UNCHECKED_CAST")
                        val textView = it.args[0] as Array<TextView>
                        textView.forEach { tv ->
                            tv.typeface = miFontTypeface(430)
                        }
                        it.result = null
                    }

                    mobileTypeDrawableClass?.methodFinder()?.filterByName("setMiuiStatusBarTypeface")?.first()?.createBeforeHook {
                        @Suppress("UNCHECKED_CAST")
                        val paint = it.args[0] as Array<Paint>
                        paint.forEach { p ->
                            p.typeface = miFontTypeface(520)
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

fun miFontTypeface(wght: Int): Typeface = Typeface.Builder("/system/fonts/MiSansVF.ttf").setFontVariationSettings("'wght' $wght").build()