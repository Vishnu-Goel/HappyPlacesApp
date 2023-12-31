package com.happyplaces.adapters

import android.app.Activity
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplacesapp.R
import com.example.happyplacesapp.activities.AddHappyPlaceActivity
import com.example.happyplacesapp.activities.MainActivity
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.models.HappyPlaceModel
import de.hdodenhof.circleimageview.CircleImageView




open class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>,

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onClickListener : OnClickListener? = null


    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view){
        lateinit var tvTitle: TextView
        lateinit var iv_place_image: CircleImageView
        lateinit var tvDescription: TextView


        init {


            tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            iv_place_image = view.findViewById<CircleImageView>(R.id.iv_place_image)
          tvDescription = view.findViewById<TextView>(R.id.tvDescription)


        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_happy_place,
                parent,
                false
            )
        )
    }
fun setOnClickListener(onClickListener: OnClickListener){
    this.onClickListener = onClickListener


}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            holder.iv_place_image.setImageURI(Uri.parse(model.image))
            holder.tvTitle.text = model.title
            holder.tvDescription.text = model.description
            holder.itemView.setOnClickListener{
                if(onClickListener !=null){
                    onClickListener!!.onClick(position,model)
                }
            }


        }
    }
fun removeAt(position: Int){
    val dbHandler = DatabaseHandler(context)
    val isDeleted = dbHandler.deleteHappyPlace(list[position])
    if(isDeleted>0){
        list.removeAt(position)
        notifyItemRemoved(position)
    }
}

fun notifyEditItem(activity : Activity, position: Int, requestCode : Int){
    val intent = Intent(context,AddHappyPlaceActivity::class.java)
    intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])
    activity.startActivityForResult(intent,requestCode)
    notifyItemChanged(position)
}




    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickListener {
        fun onClick(position: Int, model: HappyPlaceModel)
    }


}
