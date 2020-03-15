package com.example.hagome.howlstargram

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
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
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailviewFragment : Fragment() {

    // Firestore 접근
    var firestore: FirebaseFirestore? = null
    var user: FirebaseAuth? = null
    var fcmPush:FcmPush?=null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        firestore = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance()
        fcmPush = FcmPush()
        var view = LayoutInflater.from(inflater.context).inflate(R.layout.fragment_detail, container, false)

        view.detailviewfragment_recycleview.adapter = DetailRecyclerviewAdapter()
        view.detailviewfragment_recycleview.layoutManager = LinearLayoutManager(activity)

        return view;
    }

    inner class DetailRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        // 필드 상위(주소)
        val contentDTOs: ArrayList<ContentDTO>

        // 필드
        val contentUidList: ArrayList<String>

        // DB 접근
        init {

            // 초기화
            contentDTOs = ArrayList()
            contentUidList = ArrayList()

            // 현재 로그인 된 유저의 UID(개인 해쉬 키)
            var uid = FirebaseAuth.getInstance().currentUser?.uid

            firestore?.collection("users")?.document(uid!!)?.get()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            var userDTO = task.result!!.toObject(FollowDTO::class.java)

                            if (userDTO != null) {
                                getContents(userDTO.followings)
                            }

                } else return@addOnCompleteListener
            }
        }

        fun getContents(followers: MutableMap<String, Boolean>) {

            // images 폴더 접근, timestamp 기준으로 정렬
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, _ ->
                if (querySnapshot == null) return@addSnapshotListener
                // 이전 정보 삭제 후 다시 불러오기
                contentDTOs.clear()
                contentUidList.clear()
                for (snapshot in querySnapshot.documents) {
                    var item = snapshot.toObject(ContentDTO::class.java)

                    // 팔로우 한 사람 이미지 가져오기
                    if (followers.keys.contains(item!!.uid) || user?.uid == item.uid){
                        contentDTOs.add(item)
                        contentUidList.add(snapshot.id)
                    }
                }

                contentDTOs.reverse()
                contentUidList.reverse()
                notifyDataSetChanged() // 새로고침
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)

            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val viewHolder = (holder as CustomViewHolder).itemView

            // 유저 아이디
            viewHolder.detailviewitem_profile_textview.text = contentDTOs[position].userId
            // 유저 이미지
            firestore?.collection("profileImages")?.document(contentDTOs[position].uid!!)?.get()?.addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    var url = task.result!!["image"]
                    if (url==null){
                        viewHolder.detailviewitem_profile_image.setImageResource(R.drawable.ic_account_gray)
                        return@addOnCompleteListener
                    }
                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(viewHolder.detailviewitem_profile_image)
                }
            }            // 이미지
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitem_imageview_content)
            // 설명 텍스트
            viewHolder.detailviewitem_explain_textview.text = contentDTOs[position].explain
            // 좋아요 카운터
            viewHolder.detailviewitem_favoritecounter_textview.text = "좋아요  " + contentDTOs[position].favoriteCount + "개"

            //좋아요 누른사람 확인 __개
            viewHolder.detailviewitem_favoritecounter_textview.setOnClickListener { v ->
                var intent = Intent(v.context, FavoriteActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)

                activity!!.startActivityForResult(intent, 11)
            }

            if(contentDTOs[position].commentCount != 0){
                // 댓글 카운터
                viewHolder.detailviewitem_commentcounter_textview.text = "댓글 " + contentDTOs[position].commentCount + "개 모두 보기"
            }


            var uid = FirebaseAuth.getInstance().currentUser!!.uid
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            // 좋아요를 클릭 하였을 때
            if (contentDTOs[position].favorites.containsKey(uid)) {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
                // 좋아요를 클릭하지 않았을 때
            } else {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // 프로필 이동
            viewHolder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
            }

            // 덧글 __ 개
            viewHolder.detailviewitem_commentcounter_textview.setOnClickListener{ v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)

                activity!!.startActivityForResult(intent, 11)
            }

            // 덧글 이미지
            viewHolder.detailviewitem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)

                activity!!.startActivityForResult(intent, 11)
            }
        }

        private fun favoriteEvent(position: Int) {
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])

            firestore?.runTransaction { transaction ->
                var uid = FirebaseAuth.getInstance().currentUser!!.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                // 좋아요를 눌렀을 때
                if (contentDTO!!.favorites.containsKey(uid)) {
                    //좋아요를 누른상태 -> 누르지 않는 상태
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid)

                } else {

                    // 좋아요를 누르지 않았을 때 -> 누르는 상태
                    contentDTO.favorites[uid] = true
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    favoriteAlarm(contentDTOs[position].uid!!)
                }

                transaction.set(tsDoc, contentDTO)
            }
        }

        //좋아요 알람
        fun favoriteAlarm(destinationUid: String) {
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = user?.currentUser?.email
            alarmDTO.uid = user?.currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = user?.currentUser?.email + getString(R.string.alarm_favorite)
            fcmPush?.sendMessage(destinationUid,"알림 메세지 입니다.", message)
        }
    }
}