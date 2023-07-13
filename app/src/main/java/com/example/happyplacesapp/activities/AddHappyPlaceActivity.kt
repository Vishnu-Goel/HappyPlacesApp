package com.example.happyplacesapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.example.happyplacesapp.R
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var cal = Calendar.getInstance()
    lateinit var btn_save: Button
    lateinit var et_date: EditText
    lateinit var et_description: EditText
    lateinit var et_title: EditText
    lateinit var et_location: EditText
    lateinit var tv_add_image : TextView
    lateinit var iv_place_image : ImageView
    lateinit var tv_select_current_location : TextView

    private var saveImageToInternalStorage: Uri?  =null
    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0
    private var mHappyPlaceDetails : HappyPlaceModel? = null
    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)
        binding = ActivityAddHappyPlaceBinding
            .inflate(layoutInflater)
        setContentView(binding.root)
        btn_save = findViewById(R.id.btn_save)
        tv_add_image = findViewById(R.id.tv_add_image)
iv_place_image = findViewById(R.id.iv_place_image)
       et_date = findViewById(R.id.et_date)
        et_location = findViewById(R.id.et_location)
        et_description = findViewById(R.id.et_description)
        et_title = findViewById(R.id.et_title)
        tv_select_current_location = findViewById(R.id.tv_select_current_location)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,resources.getString(R.string.google_maps_api_key))
        }


        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }


        dateSetListener = DatePickerDialog.OnDateSetListener{view,year,month,dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateView()


        }
        updateDateView()
        if(mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place"
            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_location.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            iv_place_image.setImageURI(saveImageToInternalStorage)

            btn_save.text = "UPDATE"
        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)
        tv_select_current_location.setOnClickListener(this)

    }

    override fun onClick(v:View?) {
        when(v!!.id){
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select Photo from Gallery","Capture Photo From Camera")
                pictureDialog.setItems(pictureDialogItems){
                    dialog, which ->
                    when(which){
                        0 -> choosePhotoFromGallery()
                    1 -> takePhotoFromCamera()


                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save ->{
                when {
                    et_title.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Please Enter Title",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                    et_description.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Please Enter Description",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                    et_location.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Please Enter Location",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Please select an Image",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else -> {
                        val happyPlaceModel = HappyPlaceModel(
                            if(mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(), et_description.text.toString(),
                            et_date.text.toString(),et_location.text.toString(),
                            mLatitude,
                            mLongitude

                        )

                    val dbHandler = DatabaseHandler(this)


                    if(mHappyPlaceDetails == null){
                        val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                        if(addHappyPlace > 0){
                            setResult(Activity.RESULT_OK)
                            finish()
                        }

                    } else {
                        val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                        if(updateHappyPlace > 0){
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }



                    }



                }

            }

            R.id.et_location ->{
                try {

                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    // Start the autocomplete intent with a unique request code.
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)


                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location ->{
                if(!isLocationEnabled()){
                    Toast.makeText(this,"Your location provider is turned off. Please Turn it on",Toast.LENGTH_SHORT).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else{
                    Dexter.withActivity(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        .withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                if (report!!.areAllPermissionsGranted()) {

                                   requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                            ) {
                                showRationalDialogForPermission()
                            }
                        }).onSameThread()
                        .check()
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode==Activity.RESULT_OK){
            if(requestCode== GALLERY){
                if(data != null){
                    val contentURI = data.data
                    try{
                        val selectedImageBitMap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)
                        saveImageToInternalStorage= saveImageToInternalStorage(selectedImageBitMap)
                        iv_place_image.setImageBitmap(selectedImageBitMap)
                    }catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity,"Failed!",Toast.LENGTH_SHORT).show()
                    }
                }
            } else if(requestCode == CAMERA){
                val thumbnail : Bitmap = data!!.extras!!.get("data") as Bitmap
          saveImageToInternalStorage= saveImageToInternalStorage(thumbnail)
//                Log.e("Saved Image: ","Path :: $saveImageToInternalStorage")
                iv_place_image.setImageBitmap(thumbnail)
            } else if(requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place : Place = Autocomplete.getPlaceFromIntent(data!!)
                et_location.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }
    private fun takePhotoFromCamera(){
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {if(report!!.areAllPermissionsGranted())
            {
                val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(galleryIntent, CAMERA)
            }}
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>,token: PermissionToken )
            {
                showRationalDialogForPermission()
            }
        }).onSameThread().check();

    }


    private fun choosePhotoFromGallery(){
     Dexter.withActivity(this).withPermissions(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
         android.Manifest.permission.WRITE_EXTERNAL_STORAGE
     ).withListener(object : MultiplePermissionsListener{
         @SuppressLint("SuspiciousIndentation")
         override fun onPermissionsChecked(report: MultiplePermissionsReport?)
         {if(report!!.areAllPermissionsGranted())
         {
           val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
             startActivityForResult(galleryIntent, GALLERY)
         }}
         override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>,token: PermissionToken )
         {
             showRationalDialogForPermission()
         }
     }).onSameThread().check();


    }

    private fun showRationalDialogForPermission(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permisions. It can be enabled under " +
                "application settings").setPositiveButton("Go to settings")
        {_,_ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package",packageName,null)
                intent.data = uri
                startActivity(intent)
            } catch (e:ActivityNotFoundException){
                e.printStackTrace()
            }

        }.setNegativeButton("cancel"){
            dialog,which -> dialog.dismiss()
        }.show()
    }
    private fun updateDateView(){
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }
    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try {
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        } catch (e : IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val mLastLocation: Location = p0.lastLocation!!
            mLatitude = mLastLocation.latitude
            Log.i("Current Location","$mLatitude")

            mLongitude = mLastLocation.longitude

            val addressTask =
                GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)

            addressTask.setAddressListener(object :
                GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?) {
                    Log.e("Address ::", "" + address)
                    et_location.setText(address) // Address is set to the edittext
                }

                override fun onError() {
                    Log.e("Get Address ::", "Something is wrong...")
                }
            })

            addressTask.getAddress()

        }
    }

companion object {
    private const val GALLERY = 1
    private const val CAMERA = 2
    private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
}
}