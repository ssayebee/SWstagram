package com.example.hagome.howlstargram

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.hagome.howlstargram.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_grid.view.*

class GridFragment : Fragment(){

    var mainView : View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mainView = inflater.inflate(R.layout.fragment_grid, container, false)
        mainView?.gridfragment_recycler?.adapter = GridFragmentRecyclerviewAdapter()
        mainView?.gridfragment_recycler?.layoutManager = GridLayoutManager(activity, 3)

        return mainView
    }

    inner class GridFragmentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO>

        init {
            contentDTOs = ArrayList()
            FirebaseFirestore.getInstance().collection("images").orderBy("timestamp").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot==null)return@addSnapshotListener
                contentDTOs.clear()
                for(snapshot in querySnapshot!!.documents){
                    var contentDTO = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(contentDTO!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            imageView.setPadding(5, 5, 5, 5)

            return CustomViewHolder(imageView)
        }

        private inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
            imageView.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)

                fragment.arguments = bundle

                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }

        }
    }
}