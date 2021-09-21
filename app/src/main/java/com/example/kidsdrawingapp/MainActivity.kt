package com.example.kidsdrawingapp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ContentInfoCompat
import androidx.core.view.get
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private var myImageButtonCurrentPaint: ImageButton?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var drawingView = findViewById<DrawingView>(R.id.drawingView)
        var llPaintColor = findViewById<LinearLayout>(R.id.ll_paint_colors)
        var ibBrush = findViewById<ImageButton>(R.id.ib_brush)
        var ibGallery =findViewById<ImageButton>(R.id.ib_gallery)
        var ibUndo = findViewById<ImageButton>(R.id.ib_undo)
        var ibSave = findViewById<ImageButton>(R.id.ib_save)
        var flDrawingViewContainer= findViewById<FrameLayout>(R.id.drawing_view_container)

        drawingView.setSizeForBrush(20.toFloat())

        myImageButtonCurrentPaint = llPaintColor[1] as ImageButton

        myImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )


        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        ibGallery.setOnClickListener {
            if (isReadStorageAllowed()){
                //run our code to get the image from gallery
                    //intent are used to move from one screen to another
                var pickPhotoIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent,Gallery)

            }else{
                //request storage permission
                requestStoratePermission()
            }
        }

        ibUndo.setOnClickListener {
            drawingView.onClickUndo()
        }

        ibSave.setOnClickListener {
            if (isReadStorageAllowed()){
                BitMapAsyncTask(getBitMapFromView(flDrawingViewContainer)).execute()
            }else
            {
                requestStoratePermission()
            }
        }


    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)

        var ivBackground = findViewById<ImageView>(R.id.iv_background)
        // here we are changing the background of our Image View
        if (resultCode == Activity.RESULT_OK){
            if (resultCode == Gallery){
                try {
                    if (data!!.data != null){
                        ivBackground.visibility = View.VISIBLE
                        ivBackground.setImageURI(data.data)
                    }else{
                        Toast.makeText(this@MainActivity,"Error in parsing the image or its corrupted,",Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this,)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        var drawingView = findViewById<DrawingView>(R.id.drawingView)

        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)


        smallBtn.setOnClickListener {
            drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener {
            drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()

    }

    fun paintClicked(view: View){
        var drawingView = findViewById<DrawingView>(R.id.drawingView)

        if (view != myImageButtonCurrentPaint){
            val imageButton = view as ImageButton

            var colorTag = imageButton.tag.toString()
            drawingView.setColor(colorTag)
            imageButton.setImageDrawable(
              ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            myImageButtonCurrentPaint!!.setImageDrawable(( ContextCompat.getDrawable(this, R.drawable.pallet_normal) ))

            myImageButtonCurrentPaint = view
        }

    }

    private fun requestStoratePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"Need permission to add a background",Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            storagePermissionCode)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == storagePermissionCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this@MainActivity,"Permission granted now you can read the storage file",Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this@MainActivity,"Oops you just denied the permission",Toast.LENGTH_SHORT).show()
        }

    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,(android.Manifest.permission.READ_EXTERNAL_STORAGE))

        return result == PackageManager.PERMISSION_GRANTED

    }
    //No we have to change the view of our app to bitmap because bitman are images than can be saved

    private fun getBitMapFromView(view: View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable =  view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private inner class  BitMapAsyncTask(val myBitmap:Bitmap) : AsyncTask<Any,Void,String>(){

        private lateinit var myProgressDialog: Dialog
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {


            var result = ""

            if (myBitmap != null){
                try {

                    val bytes = ByteArrayOutputStream()
                    myBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "kidsDrawingApp_"
                            + System.currentTimeMillis()/1000 + ".png")

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath

                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()

                if (!result!!.isEmpty()){
                    Toast.makeText(this@MainActivity,"File saved successfully : $result",Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this@MainActivity,"Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                path,uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(Intent.createChooser(shareIntent,"Share"))
            }
            }



        private fun showProgressDialog(){
            myProgressDialog = Dialog(this@MainActivity)
            myProgressDialog.setContentView(R.layout.dialog_custom_progress)
            myProgressDialog.show()
        }
        private fun cancelProgressDialog(){
            myProgressDialog.dismiss()
            }
    }
    companion object{
        private const val storagePermissionCode = 1
        private const val Gallery = 2
    }

}