package digital.alghad.protechmusic

import org.junit.Assert.assertEquals
import org.junit.Test

class UiLanguageTest {
    @Test fun defaultsToArabicAndSupportsEnglishWithDynamicCounts() {
        val previous = UiLanguage.english
        try {
            UiLanguage.english = false
            assertEquals("الكل", UiLanguage.text("الكل"))
            assertEquals("جميع الصوتيات (42)", UiLanguage.text("جميع الصوتيات (42)"))
            UiLanguage.english = true
            assertEquals("All", UiLanguage.text("الكل"))
            assertEquals("All audio (42)", UiLanguage.text("جميع الصوتيات (42)"))
            assertEquals("Queue (3)", UiLanguage.text("قائمة الانتظار (3)"))
            assertEquals("No results", UiLanguage.text("لا توجد نتائج"))
            assertEquals("Untranslated file metadata", UiLanguage.text("Untranslated file metadata"))
        } finally {
            UiLanguage.english = previous
        }
    }

    @Test fun worksWithTrackCountsAndDynamicLabels() {
        val previous = UiLanguage.english
        try {
            UiLanguage.english = true
            assertEquals("9 tracks", UiLanguage.text("9 مقطع صوتي"))
            assertEquals("Showing 4 of 12", UiLanguage.text("عرض 4 من 12"))
            assertEquals("Artist: Artist A", UiLanguage.text("الفنان: Artist A"))
        } finally {
            UiLanguage.english = previous
        }
    }
    @Test fun dynamicTrackActionsRespectSelectedLanguageAndPreserveMetadata() {
        val previous = UiLanguage.english
        try {
            val title = "مقطع تجريبي • Track 42"
            UiLanguage.english = false
            assertEquals("تشغيل $title", UiLanguage.text("تشغيل $title"))
            assertEquals("خيارات $title", UiLanguage.text("خيارات $title"))
            assertEquals("إضافة $title إلى قائمة", UiLanguage.text("إضافة $title إلى قائمة"))
            UiLanguage.english = true
            assertEquals("Play $title", UiLanguage.text("تشغيل $title"))
            assertEquals("Options for $title", UiLanguage.text("خيارات $title"))
            assertEquals("Add $title to playlist", UiLanguage.text("إضافة $title إلى قائمة"))
            assertEquals("Play in order", UiLanguage.text("تشغيل بالترتيب"))
            assertEquals("Add to playlist", UiLanguage.text("إضافة إلى قائمة"))
        } finally {
            UiLanguage.english = previous
        }
    }

}
