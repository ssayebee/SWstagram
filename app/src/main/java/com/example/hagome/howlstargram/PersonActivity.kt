package com.example.hagome.howlstargram

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.example.hagome.howlstargram.model.AlarmDTO
import com.example.hagome.howlstargram.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_person.*
import com.example.hagome.howlstargram.R.string.post


class PersonActivity : AppCompatActivity() {
    var fcmPush: FcmPush? = null
    var acnt = 1
    var cnt = 1

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person)

        fcmPush = FcmPush()

        var contentUid: String? = null
        var imageUrl = intent.getStringExtra("imageUrl")
        var destinationUid: String = intent.getStringExtra("destinationUid")

        person_back_btn.setOnClickListener {
            finish()
        }

        FirebaseFirestore.getInstance().collection("images").orderBy("timestamp").addSnapshotListener { querySnapshot, _ ->
            if (querySnapshot == null) return@addSnapshotListener
            // 이전 정보 삭제 후 다시 불러오기

            for (snapshot in querySnapshot) {

                var item = snapshot.toObject(ContentDTO::class.java)

                if (item.imageUrl.toString().equals(imageUrl)) {
                    contentUid = snapshot.id;
                }
            }
        }

        FirebaseFirestore.getInstance().collection("images").whereEqualTo("imageUrl", imageUrl).addSnapshotListener { querySnapshot, _ ->

            if (querySnapshot == null) return@addSnapshotListener
            var item = querySnapshot.toObjects(ContentDTO::class.java).get(0)

            // 프로필 이미지
            FirebaseFirestore.getInstance().collection("profileImages").document(item?.uid!!).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var url = task.result!!["image"]
                    if (url == null) {
                        person_profile_image.setImageResource(R.drawable.ic_account_gray)
                        return@addOnCompleteListener
                    }
                    if (!this.isFinishing) {
                        Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(person_profile_image)
                    }
                }
            }

            //사진을 눌렀을때 -> 프로필로 이동
            person_profile_image.setOnClickListener {
                val resultIntent = Intent()
                var destinationUid = item.uid
                var userId = item.userId

                resultIntent.putExtra("destinationUid", destinationUid)
                resultIntent.putExtra("userId", userId)
                setResult(RESULT_OK, resultIntent)

                finish()
            }

            // 아이디
            person_profile_textview.text = item.userId

            // 큰 이미지
            if (!this.isFinishing) {
                Glide.with(this).load(item.imageUrl).into(person_imageview_content)
            }

            // 좋아요를 클릭 하였을 때
            if (item.favorites.containsKey(FirebaseAuth.getInstance().currentUser?.uid)) {
                person_favorite_imageview.setImageResource(R.drawable.ic_favorite)
                // 좋아요를 클릭하지 않았을 때
            } else {
                person_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // 좋아요 카운터
            person_favoritecounter_textview.text = "좋아요  " + item.favoriteCount.toString() + "개"


            //좋아요 누른사람 확인 __개
            person_favoritecounter_textview.setOnClickListener{ v ->
                var intent = Intent(v.context, FavoriteActivity::class.java)
                intent.putExtra("contentUid", contentUid)
                intent.putExtra("destinationUid", destinationUid)

                startActivityForResult(intent, 11)
            }

            // 내용
            person_explain_textview.text = item.explain.toString()

            // 덧글 ___ 개
            if (item.commentCount != 0) {
                person_commentcounter_textview.text = "댓글 " + item.commentCount + "개 모두 보기"
            }

            person_commentcounter_textview.setOnClickListener {
                var intent = Intent(this, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUid)
                intent.putExtra("destinationUid", destinationUid)
                var mainActivity = MainActivity()
                startActivityForResult(intent, 11)
            }

            // 덧글 이미지
            person_comment_imageview.setOnClickListener {
                var intent = Intent(this, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUid)
                intent.putExtra("destinationUid", destinationUid)
                var mainActivity = MainActivity()
                startActivityForResult(intent, 11)
            }

            // 좋아요 이미지
            person_favorite_imageview.setOnClickListener {
                // favoriteEvent(contentUid!!)

                var tsDoc = FirebaseFirestore.getInstance().collection("images").document(contentUid!!)

                FirebaseFirestore.getInstance().runTransaction { transaction ->

                    var uid = FirebaseAuth.getInstance().currentUser!!.uid
                    var contentDTO = transaction.get(tsDoc).toObject(ContentDTO::class.java)

                    // 좋아요를 눌렀을 때
                    if (contentDTO!!.favorites.containsKey(uid)) {
                        //좋아요를 누른상태 -> 누르지 않는 상태
                        contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                        contentDTO.favorites.remove(uid)

                    } else {

                        // 좋아요를 누르지 않았을 때 -> 누르는 상태
                        contentDTO.favorites[uid] = true
                        contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                        var destinationUid = contentDTO.uid

                        favoriteAlarm(destinationUid!!)
                    }

                    transaction.set(tsDoc, contentDTO)
                }
            }
        }

    }

    //    좋아요 알람
    fun favoriteAlarm(destinationUid: String) {

        var user = FirebaseAuth.getInstance()

        var alarmDTO: AlarmDTO? = null
        alarmDTO = AlarmDTO()

        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = user.currentUser?.email
        alarmDTO.uid = user.currentUser?.uid
        alarmDTO.kind = 0
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = user?.currentUser?.email + getString(R.string.alarm_favorite)
        fcmPush!!.sendMessage(destinationUid, "알림 메세지 입니다.", message)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == 11 && resultCode == Activity.RESULT_OK) {

            var intent = Intent(this, MainActivity::class.java)
            var destinationUid = data?.getStringExtra("destinationUid")
            var userId = data?.getStringExtra("userId")

            intent.putExtra("destinationUid", destinationUid)
            intent.putExtra("userId", userId)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

    }
}
