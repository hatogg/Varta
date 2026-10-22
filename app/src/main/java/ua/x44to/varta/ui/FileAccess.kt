package ua.x44to.varta.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import ua.x44to.varta.clean.Cleaner

/** Запит доступу до файлів: на Android 11+ — окреме системне вікно, раніше — звичайний дозвіл. */
object FileAccess {
    var launcher: ActivityResultLauncher<Array<String>>? = null

    /** Збільшується, коли людина повертається з налаштувань, — щоб екрани перевірили дозволи знову. */
    var tick by mutableIntStateOf(0)

    fun request(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 30) {
            Actions.open(ctx, Cleaner.filesAccessIntent(), Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } else {
            launcher?.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
}
