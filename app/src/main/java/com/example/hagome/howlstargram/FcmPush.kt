package com.example.hagome.howlstargram

import com.example.hagome.howlstargram.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException



class FcmPush(){
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https:/fcm.googleapis.com/fcm/send"
    var serverKey = "AAAASaJg1Lg:APA91bHvwiz0dExTNmzAo6Q9rlr72r3fPHFYoyCT213Pg_wiNoDDmrUkAo0UweWzL7MJuZlLvJO9kal-KMk1A7g3lGWnzC7bV6pZtRpbJeuchMIrz4UvB8OmAfxaVbwcG_iqtCoi8ogQ"

    var okHttpClient:OkHttpClient?=null
    var gson:Gson?=null
    init {
        gson=Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid:String, title:String?, message:String?){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if (task.isSuccessful){
                var token = task.result!!["pushToken"].toString()

                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification?.title = title
                pushDTO.notification?.body = message

                var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                var request = Request.Builder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "key="+serverKey)
                        .url(url)
                        .post(body)
                        .build()
                okHttpClient?.newCall(request)?.enqueue(object :Callback{
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                    }

                })

            }
        }
    }

}