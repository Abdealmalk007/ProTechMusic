package digital.alghad.protechmusic

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceClassifierTest {
    @Test fun whatsappAudio() {
        assertEquals("WhatsApp",SourceClassifier.classify("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/"))
    }
    @Test fun whatsappBusiness() {
        assertEquals("WhatsApp",SourceClassifier.classify("Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Audio/"))
    }
    @Test fun downloads() {
        assertEquals("Downloads",SourceClassifier.classify("Download/"))
        assertEquals("Downloads",SourceClassifier.classify("downloads/old/"))
    }
    @Test fun recordings() {
        assertEquals("Recordings",SourceClassifier.classify("Recordings/CallRecordings/"))
    }
    @Test fun telegram() {
        assertEquals("Telegram",SourceClassifier.classify("Android/media/org.telegram.messenger/Telegram/Telegram Audio/"))
    }
    @Test fun music() {
        assertEquals("Music",SourceClassifier.classify("Music/Rock/"))
    }
    @Test fun other() {
        assertEquals("Other",SourceClassifier.classify("Pictures/Misc/"))
    }
}
