package com.example.hagome.howlstargram

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import kotlinx.android.synthetic.main.activity_favorite.*
import kotlinx.android.synthetic.main.item_comment.view.*

class FavoriteActivity : AppCompatActivity() {
    var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        firestore = FirebaseFirestore.getInstance()

        favorite_recyclerview.adapter = FavoriteRecyclerViewAdapter()
        favorite_recyclerview.layoutManager = LinearLayoutManager(this)


        favorite_back_btn.setOnClickListener {
            finish()
        }
    }

    inner class FavoriteRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        // 필드 상위(주소)
        val emailList: ArrayList<String>
        val uidList: ArrayList<String>


        init {
            // 초기화
            emailList = ArrayList()
            uidList = ArrayList()

            var contentUid = intent.getStringExtra("contentUid")

            firestore?.collection("images")?.document(contentUid!!)?.get()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var contentDTO = task.result!!.toObject(ContentDTO::class.java)
                    if (contentDTO != null) {
                        var favorites = contentDTO.favorites
                        for (key in favorites) {
                            uidList.add(key.key)
                        }
                        notifyDataSetChanged()
                    }
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!)

        override fun getItemCount(): Int {
            return uidList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var view = holder.itemView

            var userEmail: String? = null

            //uid로 userEmail 얻기
            firestore?.collection("email")
                    ?.document(uidList[position])?.addSnapshotListener { documentSnapshot, _ ->
                        if (documentSnapshot == null) {
                            return@addSnapshotListener
                        }
                        userEmail = documentSnapshot.data!!["email"].toString()
                        view.commentviewitem_textview_profile.text = userEmail
                    }


            FirebaseFirestore.getInstance().collection("profileImages").document(uidList[position]).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var url = task.result!!["image"]
                    if (url == null) {
                        view.commentviewItem_imageview_profile.setImageResource(R.drawable.ic_account_gray)
                        return@addOnCompleteListener
                    }
                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewItem_imageview_profile)
                }
            }

            //사진을 눌렀을때 -> 프로필로 이동
            view.commentviewItem_imageview_profile.setOnClickListener {
                val resultIntent = Intent()
                var destinationUid = uidList[position]

                resultIntent.putExtra("destinationUid", destinationUid)
                resultIntent.putExtra("userId", userEmail)
                setResult(RESULT_OK, resultIntent)

                finish()
            }
        }

        fun getFavorite(followers: MutableMap<String, Boolean>) {
            for (key in followers.keys) {
                FirebaseFirestore.getInstance().collection("email")
                        .document(key).addSnapshotListener { documentSnapshot, _ ->
                            if (documentSnapshot == null) return@addSnapshotListener
                            var email = documentSnapshot.get("email").toString()

                            emailList.add(email)
                            uidList.add(key)

                            notifyDataSetChanged()
                        }
            }
        }
    }


}
