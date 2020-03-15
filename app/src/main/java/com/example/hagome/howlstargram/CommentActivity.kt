package com.example.hagome.howlstargram

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.hagome.howlstargram.model.AlarmDTO
import com.example.hagome.howlstargram.model.ContentDTO
import com.example.hagome.howlstargram.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*


class CommentActivity : AppCompatActivity() {
    var contentUid: String? = null
    var user: FirebaseAuth? = null
    var destinationUid: String? = null
    var fcmPush: FcmPush? = null
    var timestamp: String? = null



    var firestore: FirebaseFirestore? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        user = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        contentUid = intent.getStringExtra("contentUid")
        timestamp = intent.getStringExtra("timestamp")
        destinationUid = intent.getStringExtra("destinationUid")

        comment_back_btn.setOnClickListener {
            finish()
        }

        fcmPush = FcmPush()

        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        comment_btn_send.setOnClickListener {

            if(comment_edit_message.text.toString().matches("\\s+".toRegex()) || comment_edit_message.text.toString().equals("")){
                return@setOnClickListener
            }

            var comment = ContentDTO.Comment()

            comment.userId = FirebaseAuth.getInstance().currentUser!!.email
            comment.comment = comment_edit_message.text.toString()

            comment.uid = FirebaseAuth.getInstance().currentUser!!.uid
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)

            commentAlarm(destinationUid!!, comment_edit_message.text.toString())
            comment_edit_message.setText("")

            ComentEvent()
        }

    }

    fun commentAlarm(destinationUid: String, message: String) {
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = user?.currentUser?.email
        alarmDTO.uid = user?.currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var final_message = user?.currentUser?.email + getString(R.string.alarm_who) + message + getString(R.string.alarm_comment)
        fcmPush?.sendMessage(destinationUid, "알림 메세지 입니다.", final_message)

    }

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val comments: ArrayList<ContentDTO.Comment>

        init {

            comments = ArrayList()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").orderBy("timestamp").addSnapshotListener { querySnapshot, _ ->
                comments.clear()
                if (querySnapshot == null) return@addSnapshotListener

                for (snapshot in querySnapshot.documents) {
                    comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                }

                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)

            comment_recyclerview.scrollToPosition(comments.size-1)
            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var view = holder.itemView
            view.commentviewitem_textview_comment.text = comments[position].comment
            view.commentviewitem_textview_profile.text = comments[position].userId

            FirebaseFirestore.getInstance().collection("profileImages").document(comments[position].uid!!).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var url = task.result!!["image"]
                    if (url == null){
                        view.commentviewItem_imageview_profile.setImageResource(R.drawable.ic_account_gray)
                    } else{
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewItem_imageview_profile)
                    }
                }
            }

            view.commentviewItem_imageview_profile.setOnClickListener {
                val resultIntent = Intent()
                var destinationUid = comments[position].uid
                var userId = comments[position].userId

                resultIntent.putExtra("destinationUid", destinationUid)
                resultIntent.putExtra("userId", userId)
                setResult(RESULT_OK, resultIntent)
                finish()
            }

            view.comment_view.setOnClickListener {
                firestore?.collection("images")!!.document(contentUid.toString()).collection("comments").document(comments[position].uid!!).delete()

            }

        }

    }

    private fun ComentEvent() {
        var tsDoc = firestore?.collection("images")?.document(contentUid!!)

        firestore?.runTransaction { transaction ->
            var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

            contentDTO?.commentCount = contentDTO?.commentCount!! + 1

            transaction.set(tsDoc, contentDTO)
        }
    }



}
