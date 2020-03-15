package com.example.hagome.howlstargram

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.example.hagome.howlstargram.model.ContentDTO
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.util.*




class AddPhotoActivity : AppCompatActivity() {
    val PICK_IMAGE_FROM_ALBUM=0 //이미지인지 판별
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    var auth:FirebaseAuth?=null
    var firestore:FirebaseFirestore?=null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        addphoto_btn_upload.visibility = View.VISIBLE

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        //클릭시 앨범 열림
        addphoto_image.setOnClickListener {
            var intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,PICK_IMAGE_FROM_ALBUM)
        }
        addphoto_btn_upload.setOnClickListener {
            addphoto_btn_upload.visibility = View.GONE
            Toast.makeText(this, "사진을 업로드 중 입니다..", Toast.LENGTH_LONG).show()
            contentUpload()
        }
    }

    //data = 사진
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){ //사진이 선택
                photoUri = data?.data
                addphoto_image.setImageURI(data?.data)
            }else{
                finish()
            }
        }
    }
    fun contentUpload() {

        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "PNG_"+timeStamp + "_.png"
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        var uploadTask = storageRef?.putFile(photoUri!!)

        uploadTask?.continueWithTask(object : Continuation<UploadTask.TaskSnapshot, Task<Uri>> {
            @Throws(Exception::class)
            override fun then(task: Task<UploadTask.TaskSnapshot>): Task<Uri> {
                if (!task.isSuccessful) {
                    throw task.getException()!!
                }

                // Continue with the task to get the download URL
                return storageRef!!.getDownloadUrl()
            }
        })!!.addOnCompleteListener(object : OnCompleteListener<Uri> {

            override fun onComplete(task: Task<Uri>) {
                if (task.isSuccessful) {

                    Toast.makeText(applicationContext,getString(R.string.upload_success),Toast.LENGTH_LONG).show()

                    val downloadUri = task.result
                    var uri = downloadUri

                    var contentDTO = ContentDTO()
                    //이미지 주소
                    contentDTO.imageUrl = uri!!.toString()

                    //유저의 UID
                    contentDTO.uid = auth?.currentUser?.uid
                    //게시물 설명
                    contentDTO.explain = addphoto_edit_explain.text.toString()
                    //유저 아이디
                    contentDTO.userId=auth?.currentUser?.email
                    //게시물 업로드 시간
                    contentDTO.timestamp = System.currentTimeMillis()

                    firestore?.collection("images")?.document()?.set(contentDTO)
                    setResult(Activity.RESULT_OK)

                    finish()
                } else {
                    // Handle failures
                    // ...
                }
            }
        })
    }
}
