package com.example.hagome.howlstargram

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.MenuItem
import android.view.View
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    var storage: FirebaseStorage? = null
    var PICKPROFILE_FROM_ALBUM = 10
    var COMMENT_MOVE_USER = 11


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()

        when (item.itemId) {
            R.id.action_home -> {
                var detailFragment = DetailviewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailFragment).commit()
                return true
            }

            R.id.action_search -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment).commit()
                return true
            }

            R.id.action_photo -> {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
                return true
            }

            R.id.action_favorite_alarm -> {
                var alertFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alertFragment).commit()
                return true
            }

            R.id.action_account -> {
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var userFragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setToolbarDefault()

        var detailFragment = DetailviewFragment()
        supportFragmentManager.beginTransaction().replace(R.id.main_content, detailFragment).commit()

        bottom_navigation.setOnNavigationItemSelectedListener(this)

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        registerPushToken()


        var uid = FirebaseAuth.getInstance().currentUser?.uid
        var email = FirebaseAuth.getInstance().currentUser?.email
        var map = mutableMapOf<String, Any>()

        map["email"] = email!!
        FirebaseFirestore.getInstance().collection("email").document(uid!!).set(map)


    }
    fun registerPushToken(){
        var pushToken = FirebaseInstanceId.getInstance().token
        var uid = FirebaseAuth.getInstance().currentUser?.uid
        var map = mutableMapOf<String, Any>()

        map["pushToken"] = pushToken!!
        FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
    }

    fun setToolbarDefault(){
        toolbar_btn_back.visibility = View.GONE
        toolbar_username.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        storage = FirebaseStorage.getInstance()

        //댓글 -> 유저 프로필을 눌렀을때 응답
        if (requestCode == COMMENT_MOVE_USER && resultCode == Activity.RESULT_OK) {

            var fragment = UserFragment()
            var bundle = Bundle()
            var destinationUid = data?.getStringExtra("destinationUid")
            var userId = data?.getStringExtra("userId")

            bundle.putString("destinationUid", destinationUid)
            bundle.putString("userId", userId)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
        }

        if (requestCode == PICKPROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            var photoUri: Uri? = null
            photoUri = data?.data

            var uid = FirebaseAuth.getInstance().currentUser!!.uid

            val storageRef = storage?.reference?.child("userProfileImages")?.child(uid)
            var uploadTask = storageRef!!.putFile(photoUri!!)

            val urlTask = uploadTask?.continueWithTask(object : Continuation<UploadTask.TaskSnapshot, Task<Uri>> {
                @Throws(Exception::class)
                override fun then(task: Task<UploadTask.TaskSnapshot>): Task<Uri> {
                    if (!task.isSuccessful) {
                        throw task.getException()!!
                    }

                    return storageRef!!.getDownloadUrl()
                }
            })!!.addOnCompleteListener(object : OnCompleteListener<Uri> {
                override fun onComplete(task: Task<Uri>) {
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        var url = downloadUri.toString()
                        var map = HashMap<String, Any>()
                        map["image"] = url
                        FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)

                        setResult(Activity.RESULT_OK)
                    } else { }
                }
            })
        }
    }
}

