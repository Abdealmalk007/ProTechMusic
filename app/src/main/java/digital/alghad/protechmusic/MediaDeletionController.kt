package digital.alghad.protechmusic

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.util.ArrayDeque

/**
 * PERMANENT deletion, never "remove from playlist". Only accepts individual
 * MediaStore item URIs, and never touches files without user confirmation in UI.
 * Android 11+: platform consent per group. Android 10: per-item consent.
 */
class MediaDeletionController(
    private val activity: ComponentActivity,
    private val onDeleted: (Set<String>) -> Unit,
    private val onFinished: (String) -> Unit
) {
    private val pending=ArrayDeque<Uri>()
    private var inFlight:List<Uri> = emptyList()
    private var retryUri:Uri? = null
    private var successCount=0
    private var unsupportedCount=0
    private var failureCount=0
    private var waitingForLegacyPermission:List<AudioTrack> = emptyList()

    private val consent=activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if(result.resultCode!=Activity.RESULT_OK) {
            pending.clear()
            inFlight=emptyList()
            retryUri=null
            finish("ألغيت موافقة Android. لم تُحذف الملفات التي لم توافق عليها.")
        } else if(Build.VERSION.SDK_INT>=30) {
            val deleted=inFlight
            inFlight=emptyList()
            repeat(deleted.size){pending.removeFirst()}
            successCount+=deleted.size
            if(deleted.isNotEmpty())onDeleted(deleted.mapTo(linkedSetOf()){it.toString()})
            processNext()
        } else {
            val single=retryUri
            retryUri=null
            if(single!=null){
                try{
                    if(activity.contentResolver.delete(single,null,null)>0) {
                        successCount++
                        onDeleted(setOf(single.toString()))
                    } else failureCount++
                }catch (_:Exception){failureCount++}
            }
            processNext()
        }
    }
    private val legacyPermission=activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ granted ->
        val tracks=waitingForLegacyPermission
        waitingForLegacyPermission=emptyList()
        if(granted)begin(tracks)
        else onFinished("لم يتم الحذف: صلاحية الكتابة مطلوبة على Android 8/9.")
    }

    fun begin(tracks:List<AudioTrack>) {
        if(pending.isNotEmpty()||inFlight.isNotEmpty()||retryUri!=null){
            onFinished("هناك عملية حذف جارية بالفعل.")
            return
        }
        if(Build.VERSION.SDK_INT<=28 &&
            activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED) {
            waitingForLegacyPermission=tracks
            legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        val requested=tracks.mapNotNull{
            runCatching{Uri.parse(it.uri)}.getOrNull()
        }.distinct()
        val supported=requested.filter {
            it.scheme=="content" && it.authority==MediaStore.AUTHORITY &&
                it.lastPathSegment?.toLongOrNull()!=null
        }
        if(supported.isEmpty()){
            onFinished("لا توجد ملفات MediaStore قابلة للحذف. الملفات المستوردة عبر SAF تحتاج صلاحية حذف من موفر المستندات.")
            return
        }
        successCount=0
        failureCount=0
        unsupportedCount=requested.size-supported.size
        pending.addAll(supported)
        processNext()
    }

    private fun processNext(){
        if(inFlight.isNotEmpty()||retryUri!=null)return
        if(pending.isEmpty()){finish(null);return}
        if(Build.VERSION.SDK_INT>=30){
            val batch=pending.take(150) // Platform maximum is 2000; smaller consent chunks are easier to review.
            try {
                val request=MediaStore.createDeleteRequest(activity.contentResolver,batch)
                inFlight=batch
                consent.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }catch (_:Exception){
                failureCount+=batch.size
                repeat(batch.size){pending.removeFirst()}
                processNext()
            }
            return
        }
        // Android 8/9: WRITE_EXTERNAL_STORAGE consent. Android 10: recoverable per-URI request.
        while(pending.isNotEmpty()){
            val uri=pending.removeFirst()
            try {
                if(activity.contentResolver.delete(uri,null,null)>0){
                    successCount++
                    onDeleted(setOf(uri.toString()))
                } else failureCount++
            }catch(e:SecurityException){
                if(Build.VERSION.SDK_INT==29 && e is RecoverableSecurityException){
                    try {
                        retryUri=uri
                        consent.launch(IntentSenderRequest.Builder(
                            e.userAction.actionIntent.intentSender
                        ).build())
                        return
                    }catch (_:Exception){
                        retryUri=null
                        failureCount++
                    }
                }else failureCount++
            }catch (_:Exception){failureCount++}
        }
        finish(null)
    }

    private fun finish(custom:String?){
        val message=custom ?: "الحذف النهائي: نجح "+successCount+
            "، تعذر "+failureCount+"، غير مدعوم "+unsupportedCount+"."
        pending.clear()
        inFlight=emptyList()
        retryUri=null
        onFinished(message)
    }
}
