package com.example.myapplication

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Html.ImageGetter
import android.util.Base64
import android.util.Base64OutputStream
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.connectsdk.core.MediaInfo
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.command.ServiceCommandError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {
    var listDevice: MutableList<ConnectableDevice> = mutableListOf()
    lateinit var mDiscoveryManager: DiscoveryManager
    var mediaURL = ""
    var mimeType = "image/jpg"
    private val TAG = "ABC"
    val database = Firebase.database
    val myRef = database.getReference("message")

    //    private var mDevice: ConnectableDevice? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val listView: ListView by lazy { findViewById<ListView>(R.id.listView) }
        val btnOpen: Button by lazy { findViewById<Button>(R.id.btnOpen) }
//-------------------------------------------

        // Read from the database
        myRef.addValueEventListener(object: ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = snapshot.getValue<String>()
                Log.d(TAG, "Value is: " + value)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }

        })
        btnOpen.setOnLongClickListener {
            myRef.setValue("Hello, World!")
            true
        }
//-------------------------------------------
        btnOpen.setOnClickListener {
            openGalleryForVideo()
        }

//        val videoFile =Uri.parse("/sdcard/DCIM/Camera/VID_20221216_161405.mp4")
//        videoView.setVideoURI(videoFile)
//        videoView.start()
        DiscoveryManager.init(applicationContext)
        mDiscoveryManager = DiscoveryManager.getInstance()
        mDiscoveryManager.start()
        mDiscoveryManager.addListener(object : DiscoveryManagerListener {
            override fun onDeviceAdded(manager: DiscoveryManager?, device: ConnectableDevice?) {
////                Log.e("zzzzzzzzzzzzzz", device!!.)
////                if(device.serviceId != "DIAL" && device.serviceId != "DLNA"){
////                listDevice.add(device!!)
////                }
//                for (i in device!!.services){
//                    Log.e("zzzzzzzzzzzzz", i.serviceDescription.port.toString() )
//                }
                listDevice.add(device!!)
                val arrayAdapter: ArrayAdapter<*>
                arrayAdapter =
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, listDevice)
                listView.adapter = arrayAdapter


            }

            override fun onDeviceUpdated(manager: DiscoveryManager?, device: ConnectableDevice?) {

            }

            override fun onDeviceRemoved(manager: DiscoveryManager?, device: ConnectableDevice?) {

            }

            override fun onDiscoveryFailed(
                manager: DiscoveryManager?,
                error: ServiceCommandError?
            ) {

            }

        })

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { p0, p1, p2, p3 ->
                val list = p0.adapter.getItem(p2) as ConnectableDevice
                try {
                    list.disconnect()
                    list.connect()
                    val mediaInfo = MediaInfo.Builder(mediaURL, mimeType).build()
                    val listener: MediaPlayer.LaunchListener = object : MediaPlayer.LaunchListener {
                        override fun onError(error: ServiceCommandError?) {
                            Log.e("zzzzzzzzzzzzzzz", error?.message.toString())
                        }

                        override fun onSuccess(`object`: MediaPlayer.MediaLaunchObject?) {
                            Log.e("zzzzzzzzzzzzzzzz", "onSuccess: ")
                        }
                    }
                    list.mediaPlayer.displayImage(mediaInfo, listener)


                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

    }

    private fun openGalleryForVideo() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        resultLauncher.launch(intent)
    }


    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val dataUri = data?.data
                Log.e(javaClass.name, "resultLauncher: $dataUri")
                if (dataUri != null) {
                    val uriPathHelper = URIPathHelper()
                    val videoInputPath = uriPathHelper.getPath(this, dataUri).toString()
                    mimeType = "image/${if(getMimeType(dataUri)== "jpg") "jpeg" else getMimeType(dataUri)}"
                    mediaURL = "data:image/"+getMimeType(dataUri)+";base64,"+ convertImageFileToBase64(File(videoInputPath))

                }

            }
        }


    private fun convertImageFileToBase64(imageFile: File): String {
        return ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(
                outputStream,
                Base64.DEFAULT
            ).use { base64FilterStream ->
                imageFile.inputStream().use { inputStream ->
                    inputStream.copyTo(base64FilterStream)
                }
            }
            return@use outputStream.toString()
        }
    }

    private fun getMimeType(uri: Uri): String? {
        val extension: String? = if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            val mime = MimeTypeMap.getSingleton()
            mime.getExtensionFromMimeType(contentResolver.getType(uri))
        } else {
            MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(uri.path?.let { File(it) }).toString())
        }
        return extension
    }

    fun getImageHTML(): ImageGetter? {
        return ImageGetter { source ->
            try {
                val base64Image = source.substring("data:image/jpeg;base64,".length)
                val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                val drawable: Drawable = BitmapDrawable(this.resources /*or other way to get resource reference*/, bitmap)
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                return@ImageGetter drawable
            } catch (exception: IOException) {
                Log.v("IOException", exception.message.toString())
                return@ImageGetter null
            }
        }
    }

}