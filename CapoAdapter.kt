package com.example.progettopm

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CapoAdapter(private var capi: List<Capo>) :
    RecyclerView.Adapter<CapoAdapter.CapoViewHolder>() {

    class CapoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewClothing)
        val nameCapo: TextView = view.findViewById(R.id.textNameBrand)
        val categorySizeColor: TextView = view.findViewById(R.id.textCategorySizeColor)
        val brand: TextView = view.findViewById(R.id.textBrand)
        val material: TextView = view.findViewById(R.id.textMaterial)
        val season: TextView = view.findViewById(R.id.textSeason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CapoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.capo_card_item, parent, false)
        return CapoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CapoViewHolder, position: Int) {
        val capo = capi[position]

        holder.nameCapo.text = "${capo.nome}"
        holder.categorySizeColor.text = "${capo.categoria}, ${capo.taglia}, ${capo.colore}"
        holder.brand.text = capo.brand
        holder.material.text = capo.materiale
        holder.season.text = capo.stagione                                    // Stagione

        if (!capo.imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(capo.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.imageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    override fun getItemCount() = capi.size

    fun updateData(newCapi: List<Capo>) {
        capi = newCapi
        notifyDataSetChanged()
    }
}
