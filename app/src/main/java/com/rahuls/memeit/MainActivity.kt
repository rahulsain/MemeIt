package com.rahuls.memeit

import android.Manifest
import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GestureDetectorCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    private var isChecked: Boolean = false
    private lateinit var detector: GestureDetectorCompat
    private lateinit var imageBitMap: Bitmap
    private var previousImageUrl: String? = null
    private var currentImageUrl: String? = null
    private var lCurrentImageUrl: String? = null
    private var lPreviousImageUrl: String? = null
    private lateinit var mAdView: AdView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        val onOffSwitch = findViewById<View>(R.id.switchBar) as SwitchCompat
        onOffSwitch.setOnCheckedChangeListener { _ , isChecked ->
            this.isChecked = isChecked
            if (isChecked) {
                // low resolution
                Toast.makeText(this, "Less Data will be consumed", Toast.LENGTH_SHORT).show()
            } else {
                // high resolution
                Toast.makeText(this, "HD Quality Memes", Toast.LENGTH_SHORT).show()
            }
        }

        //assuming that user run this app first time
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val firstStart = prefs.getBoolean("firstStart", true)

        //checking via sharedPreferences if this app is being run first time or not
        if (firstStart) {
            showGesture()
        }

        // Initialize the Mobile Ads SDK with an AdMob App ID.
        MobileAds.initialize(this) {}

        //shows ad
        mAdView = findViewById(R.id.adView)
        // Create an ad request.
        val adRequest = AdRequest.Builder().build()
        // Start loading the ad in the background.
        mAdView.loadAd(adRequest)

        //listens for gesture made by user
        detector = GestureDetectorCompat(this, DiaryGestureListener())
        //calls the api
        loadMeme()
    }


    private fun loadLowResolutionMeme() {
        // Instantiate the RequestQueue.
        progressBar.visibility = View.VISIBLE
        val url = "https://meme-api.herokuapp.com/gimme"
        previousImageUrl = currentImageUrl
        lPreviousImageUrl = lCurrentImageUrl

        // Request a Json response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null, {
                currentImageUrl = it.getString("url")
                lCurrentImageUrl = it.optJSONArray("preview")?.getString(1)
                Glide.with(this).asBitmap().load(lCurrentImageUrl).listener(object :
                    RequestListener<Bitmap> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any?,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        imageBitMap = resource
                        progressBar.visibility = View.GONE
                        return false
                    }

                }).placeholder(R.drawable.placeholder_meme).into(memeImageView)
            },
            {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            })

        // Add the request to the RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    //show One Time Alert Box guiding user about swipes functionality
    private fun showGesture() {
        startActivity(Intent(this@MainActivity, Gesture::class.java))
        //this will make the dialog to appear only first launch
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("firstStart", false)
        editor.apply()
    }

    //calls api to load random meme
    private fun loadMeme() {
        // Instantiate the RequestQueue.
        progressBar.visibility = View.VISIBLE
        val url = "https://meme-api.herokuapp.com/gimme"
        previousImageUrl = currentImageUrl
        lPreviousImageUrl = lCurrentImageUrl

        // Request a Json response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null, {
                currentImageUrl = it.getString("url")
                lCurrentImageUrl = it.optJSONArray("preview")?.getString(1)

                Glide.with(this).asBitmap().load(currentImageUrl).listener(object :
                    RequestListener<Bitmap> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any?,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        imageBitMap = resource
                        progressBar.visibility = View.GONE
                        return false
                    }

                }).placeholder(R.drawable.placeholder_meme).into(memeImageView)
            },
            {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            })

        // Add the request to the RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    //download the image in cache
    private fun downloadThenShare(bitmap: Bitmap) {
        val fileName = "MemeIt-${System.currentTimeMillis()}.png"
        val filePath = "${this.cacheDir}/$fileName"
        download(bitmap, filePath) {
            shareImage(this, File(filePath))
        }
    }

    //download .png file
    private fun download(bitmap: Bitmap, path: String, finishDownload: () -> Unit) {
        val file = File(path)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            finishDownload.invoke()
        }
    }

    //provides sharing functionality of an image file type
    private fun shareImage(context: Context, file: File) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "image/*"
        val uri = FileProvider.getUriForFile(context, "$packageName.provider", file)
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)

        val intentChooser = Intent.createChooser(sharingIntent, "Share via")

        val resInfoList =
            packageManager.queryIntentActivities(intentChooser, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(
                packageName, uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        context.startActivity(intentChooser)
    }

    //if it is accidental tap then we deny it as a gesture and call our normal onTouch()
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (detector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    //decides in what direction the user has made a gesture
    inner class DiaryGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onFling(
            downEvent: MotionEvent?,
            moveEvent: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = moveEvent?.x?.minus(downEvent!!.x) ?: 0.0F
            val diffY = moveEvent?.y?.minus(downEvent!!.y) ?: 0.0F

            return if (abs(diffX) > abs(diffY)) {
                // this is a left or right swipe
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) {
                        // right swipe
                        this@MainActivity.onSwipeLeft()
                    } else {
                        // left swipe.
                        this@MainActivity.onSwipeRight()
                    }
                    true
                } else {
                    super.onFling(downEvent, moveEvent, velocityX, velocityY)
                }
            } else {
                // this is either a bottom or top swipe.
                if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) {
                        this@MainActivity.onSwipeBottom()
                    } else {
                        this@MainActivity.onSwipeTop()
                    }
                    true
                } else {
                    super.onFling(downEvent, moveEvent, velocityX, velocityY)
                }
            }


        }
    }

    //download image in picture folder
    private fun onSwipeBottom() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            askPermissions()
        } else {
            currentImageUrl?.let { downloadImage(it) }
        }
    }

    //share image as .png format
    private fun onSwipeTop() {
        downloadThenShare(imageBitMap)
    }

    //loads previous image
    internal fun onSwipeLeft() {
        if (isChecked) {
            // low resolution
            Glide.with(this).asBitmap().load(lPreviousImageUrl).placeholder(R.drawable.placeholder_meme).into(memeImageView)
        } else {
            // high resolution
            Glide.with(this).asBitmap().load(previousImageUrl).placeholder(R.drawable.placeholder_meme).into(memeImageView)
        }

        progressBar.visibility = View.GONE
    }

    //loads next image
    internal fun onSwipeRight() {
        if (isChecked) {
            // low resolution
            loadLowResolutionMeme()
        } else {
            // high resolution
            loadMeme()
        }
    }

    //asking for permission
    @TargetApi(Build.VERSION_CODES.M)
    fun askPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Permission required to save photos from the Web.")
                    .setPositiveButton("Allow") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                        )
                        finish()
                    }
                    .setNegativeButton("Deny") { dialog, _ -> dialog.cancel() }
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                )
                // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.

            }
        } else {
            // Permission has already been granted
            currentImageUrl?.let { downloadImage(it) }
        }
    }

    //if denied then request permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    // Download the Image
                    currentImageUrl?.let { downloadImage(it) }
                }
//                else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
//                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    private var msg: String? = ""
    private var lastMsg = ""

    //download in picture folder
    private fun downloadImage(url: String) {
        val directory = File(Environment.DIRECTORY_PICTURES)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(url.substring(url.lastIndexOf("/") + 1))
                .setDescription("")
                .setDestinationInExternalPublicDir(
                    directory.toString(),
                    url.substring(url.lastIndexOf("/") + 1)
                )
        }

        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)
        Thread {
            var downloading = true
            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                msg = statusMessage(url, directory, status)
                if (msg != lastMsg) {
                    this.runOnUiThread {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    lastMsg = msg ?: ""
                }
                cursor.close()
            }
        }.start()
    }

    //shows status of the downloading file
    private fun statusMessage(url: String, directory: File, status: Int): String {
        return when (status) {
            DownloadManager.STATUS_FAILED -> "Download has been failed, please try again"
            DownloadManager.STATUS_PAUSED -> "Paused"
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Downloading..."
            DownloadManager.STATUS_SUCCESSFUL -> "Image downloaded successfully in $directory" + File.separator + url.substring(
                url.lastIndexOf("/") + 1
            )
            else -> "There's nothing to download"
        }
    }

    override fun onStop() {
        Thread {
            // This method must be called on a background thread.
            Glide.get(this).clearDiskCache()
        }.start()
        super.onStop()
    }

    override fun onDestroy() {
        Glide.get(this).clearMemory()
        super.onDestroy()
    }


    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }

}