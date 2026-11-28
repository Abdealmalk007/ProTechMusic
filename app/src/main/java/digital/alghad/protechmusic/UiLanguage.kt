package digital.alghad.protechmusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Small offline-only UI localization layer.
 * The default remains Arabic, while an explicit toggle persists the selected language.
 */
object UiLanguage {
    var english by mutableStateOf(false)

    private val translations = mapOf(
        "مشغل الصوتيات • ALGHAD DIGITAL" to "Audio player • ALGHAD DIGITAL",
        "الكل" to "All",
        "المصادر" to "Sources",
        "قوائمي" to "Playlists",
        "المفضلة" to "Favorites",
        "الصوت" to "Sound",
        "السماح بقراءة الصوتيات" to "Allow audio access",
        "امنح صلاحية الصوتيات لعرض جميع الملفات الصوتية المتاحة، بما فيها مقاطع WhatsApp والتنزيلات والتسجيلات." to "Grant audio access to discover available WhatsApp, Downloads and recordings.",
        "منح صلاحية الصوتيات" to "Allow audio",
        "اختيار ملفات يدويًا" to "Select files manually",
        "+ استيراد" to "+ Import",
        "قوائم ذكية" to "Smart lists",
        "حسب التطبيق" to "By app",
        "حسب المجلد" to "By folder",
        "الفنانون" to "Artists",
        "الألبومات" to "Albums",
        "إلغاء التحديد" to "Clear selection",
        "تحديد متعدد" to "Select multiple",
        "‹ الرجوع إلى المصادر" to "‹ Back to sources",
        "قوائم التشغيل المخصصة" to "Your playlists",
        "البحث والفلاتر" to "Search and filters",
        "إخفاء البحث والفلاتر" to "Hide search and filters",
        "+ إنشاء" to "+ Create",
        "تصدير نسخة احتياطية" to "Export backup",
        "استيراد نسخة" to "Import backup",
        "استيراد M3U8" to "Import M3U8",
        "الصوتيات حسب التطبيق والمصدر" to "Audio by source",
        "واتساب" to "WhatsApp",
        "التنزيلات" to "Downloads",
        "التسجيلات" to "Recordings",
        "تيليجرام" to "Telegram",
        "بلوتوث" to "Bluetooth",
        "الموسيقى" to "Music",
        "المستوردة يدويًا" to "Imported",
        "قائمة التشغيل" to "Playlist",
        "تشغيل القائمة بالترتيب" to "Play in order",
        "تشغيل القائمة عشوائيًا" to "Shuffle playlist",
        "تصدير M3U8" to "Export M3U8",
        "مشاركة القائمة" to "Share playlist",
        "تعديل" to "Edit",
        "حذف القائمة فقط" to "Delete playlist only",
        "تحديد مقاطع" to "Select tracks",
        "قوائم التشغيل الذكية" to "Smart playlists",
        "ترتيب تلقائي حسب إضافة الملفات والاستماع الفعلي" to "Automatic order by added date and listening history",
        "المضافة حديثًا" to "Recently added",
        "المشغلة مؤخرًا" to "Recently played",
        "الأكثر تشغيلًا" to "Most played",
        "يُحتسب التشغيل بعد سماع جزء كافٍ من المقطع، وليس بمجرد النقر عليه." to "A play is counted only after meaningful listening.",
        "المفضلة ♥" to "Favorites ♥",
        "تحديد" to "Select",
        "بحث في الصوتيات" to "Search audio",
        "فرز" to "Sort",
        "ترتيب القائمة" to "Playlist order",
        "اسم المقطع" to "Track name",
        "الفنان" to "Artist",
        "المصدر" to "Source",
        "الأحدث إضافة" to "Recently added",
        "المدة (الأطول)" to "Duration (longest)",
        "الحجم (الأكبر)" to "Size (largest)",
        "إضافة إلى قائمة" to "Add to playlist",
        "مشاركة" to "Share",
        "تشغيل بالترتيب" to "Play in order",
        "تشغيل عشوائي" to "Shuffle",
        "حذف نهائي" to "Permanently delete",
        "لا توجد صوتيات متاحة. جرّب الاستيراد أو منح الصلاحيات." to "No audio available. Import files or grant permission.",
        "لا توجد نتائج" to "No results",
        "عرض المزيد" to "Show more",
        "عرض الكل" to "Show all",
        "إزالة من المفضلة" to "Remove favorite",
        "إضافة إلى المفضلة" to "Add favorite",
        "مشاركة المقطع" to "Share track",
        "تشغيل التالي" to "Play next",
        "إضافة إلى قائمة الانتظار" to "Add to queue",
        "حذف الملف نهائيًا من الجهاز" to "Permanently delete file",
        "تحريك لأعلى" to "Move up",
        "تحريك لأسفل" to "Move down",
        "إزالة من القائمة فقط" to "Remove from playlist only",
        "اضغط لفتح المشغل" to "Tap to open player",
        "المقطع السابق" to "Previous track",
        "إيقاف مؤقت" to "Pause",
        "تشغيل" to "Play",
        "المقطع التالي" to "Next track",
        "غلاف المقطع" to "Track artwork",
        "كلمات الأغنية LRC" to "LRC lyrics",
        "استيراد LRC" to "Import LRC",
        "يمكن اختيار ملف LRC متزامن مع هذا المقطع" to "Import a synchronized LRC file for this track",
        "خلط ✓" to "Shuffle ✓",
        "خلط" to "Shuffle",
        "تكرار 1" to "Repeat one",
        "تكرار الكل" to "Repeat all",
        "بدون تكرار" to "Repeat off",
        "مضخم الصوت والمؤثرات" to "Audio effects",
        "المؤثرات تعمل على جلسة هذا التطبيق فقط. تضخيم الصوت قد يسبب تشويهًا أو ضررًا للسمع؛ ابدأ بمستوى منخفض." to "Effects apply to this app only. High volume may distort audio or damage hearing.",
        "شغّل مقطعًا لتهيئة جلسة الصوت" to "Play a track to initialize audio effects",
        "الحد البرمجي: +6 dB؛ قد يكون التأثير غير متاح على بعض الأجهزة." to "Software cap: +6 dB. Availability depends on hardware.",
        "المؤثرات بانتظار اتصال المحرك" to "Waiting for audio engine",
        "ضبط الترددات يدويًا" to "Manual EQ bands",
        "الضبط اليدوي غير مدعوم على هذا الجهاز" to "Manual EQ unavailable on this device",
        "نطاق الضبط محدود إلى ±6 dB لتقليل التشويه" to "EQ limited to ±6 dB to reduce distortion",
        "مؤقت النوم" to "Sleep timer",
        "إيقاف" to "Off",
        "مضخم الصوت بحد أقصى +6 dB، ومجال EQ بحد ±6 dB أو أقل إذا فرض الجهاز ذلك. الدعم والتأثير الفعلي يختلفان باختلاف الهاتف والخرج الصوتي." to "Audio boost is capped at +6 dB and EQ to ±6 dB or less. Availability and output vary by device.",
        "نتيجة استيراد M3U8" to "M3U8 import results",
        "إغلاق" to "Close",
        "حذف نهائي من الجهاز" to "Permanently delete",
        "تأكيد الحذف النهائي" to "Confirm permanent deletion",
        "إلغاء" to "Cancel",
        "تأكيد حذف القائمة" to "Confirm playlist deletion",
        "سيتم حذف قائمة التشغيل فقط، ولن تُحذف الملفات الصوتية الأصلية." to "Only the playlist will be removed. Audio files remain intact.",
        "حذف القائمة" to "Delete playlist",
        "تعديل اسم القائمة" to "Rename playlist",
        "قائمة تشغيل جديدة" to "New playlist",
        "اسم القائمة" to "Playlist name",
        "حفظ" to "Save",
        "إضافة إلى قائمة تشغيل" to "Add to playlist",
        "+ إنشاء قائمة جديدة" to "+ New playlist"
    )

    fun text(arabic: String): String {
        if (!english) return arabic
        translations[arabic]?.let { return it }
        return when {
            arabic.startsWith("إضافة ") && arabic.endsWith(" إلى قائمة") ->
                "Add " + arabic.removePrefix("إضافة ").removeSuffix(" إلى قائمة") + " to playlist"
            arabic.startsWith("تشغيل ") -> "Play " + arabic.removePrefix("تشغيل ")
            arabic.startsWith("خيارات ") -> "Options for " + arabic.removePrefix("خيارات ")
            arabic.startsWith("جميع الصوتيات (") -> arabic.replaceFirst("جميع الصوتيات", "All audio")
            arabic.startsWith("قائمة الانتظار (") -> arabic.replaceFirst("قائمة الانتظار", "Queue")
            arabic.startsWith("كل الصوتيات (") -> arabic.replaceFirst("كل الصوتيات", "All tracks")
            arabic.startsWith("قائمة انتظار التشغيل (") -> arabic.replaceFirst("قائمة انتظار التشغيل", "Playback queue")
            arabic.startsWith("المجلدات (") -> arabic.replaceFirst("المجلدات", "Folders")
            arabic.startsWith("المصدر: ") -> arabic.replaceFirst("المصدر: ", "Source: ")
            arabic.startsWith("المجلد: ") -> arabic.replaceFirst("المجلد: ", "Folder: ")
            arabic.startsWith("الفنان: ") -> arabic.replaceFirst("الفنان: ", "Artist: ")
            arabic.startsWith("الألبوم: ") -> arabic.replaceFirst("الألبوم: ", "Album: ")
            arabic.startsWith("عرض ") && arabic.contains(" من ") ->
                arabic.replaceFirst("عرض ", "Showing ").replaceFirst(" من ", " of ")
            arabic.endsWith(" مقطع صوتي") -> arabic.removeSuffix(" مقطع صوتي") + " tracks"
            arabic.endsWith(" مقطع محدد") -> arabic.removeSuffix(" مقطع محدد") + " selected"
            arabic.endsWith(" مقطع") -> arabic.removeSuffix(" مقطع") + " tracks"
            arabic.endsWith(" دقيقة") -> arabic.removeSuffix(" دقيقة") + " min"
            arabic.endsWith(" تشغيل") -> arabic.removeSuffix(" تشغيل") + " plays"
            else -> arabic
        }
    }
}
