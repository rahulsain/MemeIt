package com.rahuls.memeit

import android.Manifest
import android.R.attr.*
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
import android.text.InputType
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
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
import com.bumptech.glide.load.resource.gif.GifDrawable
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

    private var isGif: Boolean = false
    private lateinit var gifBitMap: GifDrawable
    private var notSafeForWorkstatus: Boolean = true
    private lateinit var author: String
    private lateinit var mTitle: String
    private lateinit var subreddit: String
    private var notSafeForWork: Boolean = false
    private lateinit var url: String
    private var isChecked: Boolean = false
    private lateinit var detector: GestureDetectorCompat
    private lateinit var imageBitMap: Bitmap
    private var previousImageUrl: String? = null
    private var currentImageUrl: String? = null
    private var lCurrentImageUrl: String? = null
    private var lPreviousImageUrl: String? = null
    private lateinit var mAdView: AdView
    private var msg: String? = ""
    private var lastMsg = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        val onOffSwitch = findViewById<View>(R.id.switchBar) as SwitchCompat
        onOffSwitch.setOnCheckedChangeListener { _, isChecked ->
            this.isChecked = isChecked

            if (isChecked)
            // low resolution
                Toast.makeText(this, "Less Data will be consumed", Toast.LENGTH_SHORT).show()
            else
            // high resolution
                Toast.makeText(this, "Quality has been set to High", Toast.LENGTH_SHORT).show()

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
        url = "https://meme-api.herokuapp.com/gimme/"
        loadMeme(url)
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
    private fun loadMeme(apiURL: String) {
        // Instantiate the RequestQueue.
        progressBar.visibility = View.VISIBLE
        if (notSafeForWorkstatus || !notSafeForWork) {
            previousImageUrl = currentImageUrl
            lPreviousImageUrl = lCurrentImageUrl
        }
        // Request a Json response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, apiURL, null, {
                currentImageUrl = it.getString("url")
                lCurrentImageUrl = try {
                    it.optJSONArray("preview")?.getString(1)
                } catch (e: Exception) {
                    it.optJSONArray("preview")?.getString(0)
                }
                subreddit = it.getString("subreddit")
                mTitle = it.getString("title")
                notSafeForWork = it.getBoolean("nsfw")
                author = it.getString("author")

                var memeUrl = if (isChecked) lCurrentImageUrl else currentImageUrl

                if (notSafeForWork) {
                    if (!notSafeForWorkstatus) {
                        //user does not want to see nsfw content
                        memeUrl = R.drawable.placeholder_meme.toString()

                        Toast.makeText(
                            this,
                            "Not Safe For Work content ahead! Exit Subreddit mode to redirect back to memes or swipe left",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                    showWarning()
                }

                if (memeUrl!!.contains(".gif")) {
                    //display gif
                    isGif = true
                    Glide.with(this).asGif().load(currentImageUrl).listener(object :
                        RequestListener<GifDrawable> {

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<GifDrawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            progressBar.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: GifDrawable,
                            model: Any?,
                            target: Target<GifDrawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            gifBitMap = resource
                            progressBar.visibility = View.GONE
                            return false
                        }

                    }).placeholder(R.drawable.gif_placeholder).into(memeImageView)

                    Toast.makeText(
                        this,
                        "Lite mode not supported for GIF Image, please wait gif is loading",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    //display jpg
                    isGif = false
                    Glide.with(this).asBitmap().load(memeUrl).listener(object :
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
                }
            },
            {
                Toast.makeText(this, "Something went wrong! Try again", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            })

        // Add the request to the RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    private fun showWarning() {
        val mBuilder = AlertDialog.Builder(this)
        val mView: View = layoutInflater.inflate(R.layout.warning_dialog, null)

        val mCheckBox = mView.findViewById<CheckBox>(R.id.checkBox)

        mBuilder.setTitle("Not Safe For Work Content")
        mBuilder.setMessage(
            "Warning! This image may contain some graphical/violent/pornographic content.\n\n" +
                    "It may also contain hate towards a country/gender/race/community/person\n\n" +
                    "By Accepting this, You will be fully responsible, and you cannot blame this app or developer by any means.\n\n" +
                    "By using this app, you are also abiding to Terms and Condition set by the company\n\n" +
                    "Click Agree if you want to proceed or else press cancel"
        )

        mBuilder.setView(mView)

        mBuilder.setPositiveButton("Agree") { dialogInterface, _ ->
            notSafeForWorkstatus = true
            dialogInterface.dismiss()
        }
        mBuilder.setNegativeButton("Cancel") { dialogInterface, _ ->
            loadMeme("https://meme-api.herokuapp.com/gimme/")
            notSafeForWorkstatus = false
            dialogInterface.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()

        mCheckBox.setOnCheckedChangeListener { compoundButton, _ ->
            if (compoundButton.isChecked) {
                storeDialogStatus(true)
            } else {
                storeDialogStatus(false)
            }
        }

        if (getDialogStatus()) {
            mDialog.hide()
        } else {
            mDialog.show()
        }
    }

    private fun storeDialogStatus(isChecked: Boolean) {
        val mSharedPreferences = getSharedPreferences("CheckItem", MODE_PRIVATE)
        val mEditor = mSharedPreferences.edit()
        mEditor.putBoolean("item", isChecked)
        mEditor.apply()
    }

    private fun getDialogStatus(): Boolean {
        val mSharedPreferences = getSharedPreferences("CheckItem", MODE_PRIVATE)
        return mSharedPreferences.getBoolean("item", false)
    }

    //download the image in cache
    private fun downloadImageThenShare(bitmap: Bitmap) {
        val fileName = "MemeIt-${System.currentTimeMillis()}.png"
        val filePath = "${this.cacheDir}/$fileName"
        downloadImageIntoCache(bitmap, filePath) {
            shareImage(this, File(filePath))
        }
    }

    //download .png file
    private fun downloadImageIntoCache(bitmap: Bitmap, path: String, finishDownload: () -> Unit) {
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
                        // left swipe
                        this@MainActivity.onSwipeLeft()
                    } else {
                        // right swipe.
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

        override fun onLongPress(e: MotionEvent?) {
            showMemeDetail()
            super.onLongPress(e)
        }
    }

    private fun showMemeDetail() {
        AlertDialog.Builder(this)
            .setTitle("Subreddit: $subreddit")
            .setMessage("Title: $mTitle\n\nAuthor: $author")
            .show()
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
        if(isGif)
            Toast.makeText(
                this,
                "This feature is not supported!. Download and then share manually",
                Toast.LENGTH_LONG
            ).show()
        else
            downloadImageThenShare(imageBitMap)

    }

    //loads previous image
    internal fun onSwipeLeft() {
        val pURL: String? = if (isChecked)
        // low resolution
            lPreviousImageUrl
        else
        // high resolution
            previousImageUrl

        Glide.with(this).asBitmap().load(pURL).placeholder(R.drawable.placeholder_meme)
            .into(memeImageView)
        progressBar.visibility = View.GONE
    }

    //loads next image
    internal fun onSwipeRight() {
        loadMeme(url)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.open_subreddit ->
                showCustomDialogBox()
            R.id.exit_subreddit -> {
                Toast.makeText(this, "Back to memes", Toast.LENGTH_SHORT).show()
                url = "https://meme-api.herokuapp.com/gimme/"
                loadMeme(url)
            }
            else -> {
                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCustomDialogBox() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Title").setMessage("Subreddit should not be Locked or Private")

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(50, 0, 50, 0)
        val input = EditText(this)
        input.layoutParams = lp
        input.gravity = Gravity.TOP or Gravity.START
        input.setLines(1)
        input.maxLines = 1
        input.hint = "Enter Subreddit Name Eg:memes"
        input.inputType = InputType.TYPE_CLASS_TEXT
        container.addView(input, lp)

        builder.setView(container)

        // Set up the buttons
        builder.setPositiveButton("OK") { _, _ ->
            // Here you get get input text from the Edittext
            url = "https://meme-api.herokuapp.com/gimme/" + input.text.toString()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }

}