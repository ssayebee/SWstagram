package com.example.hagome.howlstargram

import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.hagome.howlstargram.model.AlarmDTO
import com.example.hagome.howlstargram.model.ContentDTO
import com.example.hagome.howlstargram.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import android.content.DialogInterface


class UserFragment : Fragment() {

    var fragmentView: View? = null
    var PICKPROFILE_FROM_ALBUM = 10
    var firestore: FirebaseFirestore? = null
    //현재 나의 uid
    var currentUserUid: String? = null
    //내가 선택한 uid
    var uid: String? = null

    var auth: FirebaseAuth? = null
    var fcmPush: FcmPush? = null

    var followListenerRegistration: ListenerRegistration? = null
    var followingListenerRegistration: ListenerRegistration? = null
    var imageprofileListenerRegistration: ListenerRegistration? = null
    var recyclerListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        firestore = FirebaseFirestore.getInstance()
        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)
        auth = FirebaseAuth.getInstance()
        fcmPush = FcmPush()

        if (arguments != null) {
            uid = arguments!!.getString("destinationUid")
            if (uid != null && uid == currentUserUid) {
                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.VISIBLE
                mainActivity.toolbar_btn_back.visibility = View.GONE
                mainActivity.toolbar_username.visibility = View.GONE
                //나의 유저페이지

                fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    activity?.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth?.signOut()
                }

                // 앨범 삽입
                fragmentView?.account_iv_profile?.setOnClickListener {
                    var photoPickerIntent = Intent(Intent.ACTION_PICK)
                    photoPickerIntent.type = "image/*"
                    activity?.startActivityForResult(photoPickerIntent, PICKPROFILE_FROM_ALBUM)
                }

            } else {
                //제3자의 유저페이지
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)

                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE
                mainActivity.toolbar_username.text = arguments!!.getString("userId")
                mainActivity.toolbar_btn_back.setOnClickListener {

                    mainActivity.bottom_navigation.selectedItemId = R.id.action_home
                }
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    requestFollow()
                }
            }

            fragmentView?.following_check_btn?.setOnClickListener { v ->
                var intent = Intent(v.context, FollowingActivity::class.java)
                intent.putExtra("uid", uid)
                activity!!.startActivityForResult(intent, 11)
            }

            fragmentView?.follower_check_btn?.setOnClickListener { v ->
                var intent = Intent(v.context, FavoriteActivity::class.java)
                intent.putExtra("uid", uid)
                activity!!.startActivityForResult(intent, 11)
            }

        }

        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdpater()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3) as RecyclerView.LayoutManager?

        return fragmentView
    }


    override fun onResume() {
        super.onResume()

        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdpater()
        getFollower()
        getFollowing()
        getProfileImage()
    }

    fun requestFollow() {

        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)

        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                followerAlarm(uid!!)
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            // Unstar the post and remove self from stars
            if (followDTO.followings.containsKey(uid)) {

                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)
            } else {

                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true


                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {


                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {

                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true

            }// Star the post and add self to stars

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }

    }

    // 사진 보이기
    fun getProfileImage() {
        imageprofileListenerRegistration = firestore?.collection("profileImages")
                ?.document(uid!!)?.addSnapshotListener { documentSnapshot, _ ->
                    if (documentSnapshot == null) {
                        return@addSnapshotListener
                    }
                    if (documentSnapshot.data == null) {
                        fragmentView!!.account_iv_profile.setImageResource(R.drawable.ic_account_gray)
                    } else {
                        var url = documentSnapshot.data!!["image"]
                        Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView!!.account_iv_profile)
                    }
                }
    }

    fun followerAlarm(destinationUid: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser!!.email
        alarmDTO.uid = auth?.currentUser!!.uid
        alarmDTO.kind = 2
        alarmDTO.message = null
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        fcmPush?.sendMessage(destinationUid, "알림 메세지 입니다.", message)

    }

    fun getFollower() {
        followListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, _ ->
            if (documentSnapshot == null) return@addSnapshotListener
            val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView?.account_tv_follower_count?.text = followDTO.followerCount.toString()
        }
    }

    fun getFollowing() {
        followingListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, _ ->
            if (documentSnapshot == null) return@addSnapshotListener
            val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView?.account_tv_following_count?.text = followDTO.followingCount.toString()


            if (followDTO.followers.containsKey(currentUserUid)) {
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
            } else {

                if (uid != currentUserUid) {

                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                }
            }

        }
    }


    inner class UserFragmentRecyclerViewAdpater : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO>

        init {

            contentDTOs = ArrayList()

            //나의 사진만 찾기
            recyclerListenerRegistration = firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, _ ->
                if (querySnapshot == null) return@addSnapshotListener
                contentDTOs.clear()
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }

                account_tv_post_count.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageview = ImageView(parent.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            imageview.setPadding(5, 5, 5, 5)

            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageview

            imageview.setOnClickListener { v ->
                var intent = Intent(v.context, PersonActivity::class.java)
                intent.putExtra("imageUrl", contentDTOs[position].imageUrl)
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                activity!!.startActivityForResult(intent, 11)
            }

            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)


            //나의 유저페이지 일때 이미지 삭제
            if (uid != null && uid == currentUserUid) {
                imageview.setOnLongClickListener {
                    val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                firestore?.collection("images")?.whereEqualTo("imageUrl", contentDTOs[position].imageUrl)?.get()?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        for (snapshot in task.result!!) {
                                            var contentUid = snapshot.id.toString()
                                            firestore?.collection("images")?.document(contentUid)?.delete()
                                            FirebaseStorage.getInstance().getReferenceFromUrl(contentDTOs[position].imageUrl!!).delete()
                                        }
                                        notifyDataSetChanged()
                                    }
                                }
                            }

                            DialogInterface.BUTTON_NEGATIVE -> {
                            }
                        }//Yes button clicked
                        //No button clicked
                    }

                    val builder = AlertDialog.Builder(context)
                    builder.setMessage("사진을 지우시겠습니까?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show()

                    true
                }
            }
        }


    }


    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
        followingListenerRegistration?.remove()
        imageprofileListenerRegistration?.remove()
        recyclerListenerRegistration?.remove()
    }

}