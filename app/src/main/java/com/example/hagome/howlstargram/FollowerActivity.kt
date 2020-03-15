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
import com.example.hagome.howlstargram.model.ContentDTO
import com.example.hagome.howlstargram.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_follower.*
import kotlinx.android.synthetic.main.activity_following.*
import kotlinx.android.synthetic.main.item_comment.view.*

class FollowerActivity : AppCompatActivity() {
    var uid: String? = null
    var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follower)
        firestore = FirebaseFirestore.getInstance()

        follower_recyclerview.adapter = FollowerRecyclerViewAdapter()
        follower_recyclerview.layoutManager = LinearLayoutManager(this)

        follower_back_btn.setOnClickListener {
            finish()
        }
    }

    inner class FollowerRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        // 필드 상위(주소)
        val emailList:ArrayList<String>
        val uidList:ArrayList<String>



        init {
            // 초기화
            emailList = ArrayList()
            uidList = ArrayList()

            var uid = intent.getStringExtra("uid")

            firestore?.collection("users")?.document(uid!!)?.get()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var userDTO = task.result!!.toObject(FollowDTO::class.java)
                    if (userDTO != null) {
                        getFollowers(userDTO.followers)
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
            return emailList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var view = holder.itemView
            view.commentviewitem_textview_profile.text = emailList[position]

            FirebaseFirestore.getInstance().collection("profileImages").document(uidList[position]).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var url = task.result!!["image"]
                    if(url == null){
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
                var userId = emailList[position]

                resultIntent.putExtra("destinationUid", destinationUid)
                resultIntent.putExtra("userId", userId)
                setResult(RESULT_OK, resultIntent)

                finish()
            }
        }

        fun getFollowers(followers: MutableMap<String, Boolean>) {
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
