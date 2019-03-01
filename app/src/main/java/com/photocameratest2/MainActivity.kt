package com.photocameratest2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    //设置权限请求和活动请求的请求码requestCode
    companion object {
        private const val PERMISSIONS_REQUEST_ALBUM = 1
        private const val PERMISSIONS_REQUEST_CAMERA = 2

        private const val ACTIVITY_REQUEST_ALBUM = 3
        private const val ACTIVITY_REQUEST_CAMERA = 4
    }

    lateinit var cameraSavePath: File
    lateinit var uri: Uri
    lateinit var imageView: ImageView

    //拍照需要的两个权限
    private val permissionList = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    //存储用户拒绝授权的权限
    var permissionTemp: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.iv_image_view)

        //拍照的文件存储位置
        cameraSavePath =
            File(Environment.getExternalStorageDirectory().path + "/" + System.currentTimeMillis() + ".jpg")

        initListener()
    }

    //设置两个button的监听
    private fun initListener() {

        //相册监听，检查一个权限
        btn_album.setOnClickListener {
            //检查版本是否大于M
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_ALBUM
                    )
                } else {
                    //权限已经被授权，开启相册
                    goAlbum()
                }
            }
        }

        //拍照监听，检查两个权限
        btn_camera.setOnClickListener {
            permissionTemp.clear()
            for (i in permissionList.indices) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permissionList[i]
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionTemp.add(permissionList[i])
                }
            }
            if (permissionTemp.isEmpty()) {
                //未授予的权限为空，表示都授予了，开启照相功能
                goCamera()
            } else {//请求权限方法
                val permissions = permissionTemp.toTypedArray()//将List转为数组
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    PERMISSIONS_REQUEST_CAMERA
                )
            }
        }
    }

    //权限结果回调
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        when (requestCode) {

            //相册权限请求结果
            PERMISSIONS_REQUEST_ALBUM -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goAlbum()
                } else {
                    Toast.makeText(this, "你拒绝了读取相册权限", Toast.LENGTH_SHORT).show()
                }
            }

            //拍照权限请求结果
            PERMISSIONS_REQUEST_CAMERA -> {
                //用于判断是否有未授权权限，没有则开启照相
                var isAgree = true
                for (i in grantResults.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        //检查到有未授予的权限
                        isAgree = false
                        //判断是否勾选禁止后不再询问
                        val showRequestPermission =
                            ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])
                        if (showRequestPermission) {
                            Toast.makeText(this, "你拒绝了拍照相关权限", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                //isAgree没有被置为false则表示权限都已授予，开启拍照
                if (isAgree) {
                    goCamera()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //相册功能
    private fun goAlbum() {
        val intent = Intent()
        intent.action = Intent.ACTION_PICK
        intent.type = "image/*"
        startActivityForResult(intent, ACTIVITY_REQUEST_ALBUM)
    }

    //拍照功能
    private fun goCamera() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, "com.photocameratest2.fileprovider", cameraSavePath)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        } else {
            uri = Uri.fromFile(cameraSavePath)
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        this.startActivityForResult(intent, ACTIVITY_REQUEST_CAMERA)
    }

    //活动请求的回调，用requestCode来匹配
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        //图片路径
        var photoPath: String = ""

        //相册
        if (requestCode == ACTIVITY_REQUEST_ALBUM && resultCode == Activity.RESULT_OK) {

            photoPath = GetPhotoFromAlbum.getRealPathFromUri(this, data!!.data)!!
            imageView.setImageURI(Uri.parse(photoPath))

            //拍照
        } else if (requestCode == ACTIVITY_REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {

            photoPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cameraSavePath.toString()
            } else {
                uri.encodedPath
            }
            imageView.setImageURI(Uri.parse(photoPath))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
