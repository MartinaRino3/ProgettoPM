package com.example.progettopm

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GuardarobaFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CapoAdapter
    private var capiList = mutableListOf<Capo>()
    private lateinit var textViewEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.guardaroba_fragment, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewGuardaroba)
        textViewEmpty = view.findViewById(R.id.textViewEmpty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CapoAdapter(capiList)
        recyclerView.adapter = adapter

        // 🔹 Aggiungi swipe con effetto cestino
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            // 🔸 Disegna effetto visivo durante lo swipe
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()

                // Disegna sfondo rosso
                paint.color = Color.parseColor("#D32F2F") // rosso Material
                if (dX > 0) { // swipe a destra
                    c.drawRect(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat(), paint
                    )
                } else { // swipe a sinistra
                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                    )
                }

                // Disegna icona cestino
                val icon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_icon)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                    val iconBottom = iconTop + it.intrinsicHeight

                    if (dX > 0) {
                        // Swipe a destra → cestino a sinistra
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    } else {
                        // Swipe a sinistra → cestino a destra
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                    it.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val capo = capiList[position]
                mostraDialogConferma(position, capo)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        caricaCapi()
        return view
    }

    private fun caricaCapi() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("guardaroba")
            .get()
            .addOnSuccessListener { documents ->
                capiList.clear()
                for (doc in documents) {
                    val capo = doc.toObject(Capo::class.java)
                    capiList.add(capo)
                }
                adapter.updateData(capiList)
                aggiornaVisibilita()
            }
            .addOnFailureListener {
                textViewEmpty.text = "Errore nel caricamento del guardaroba"
                textViewEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }

    private fun mostraDialogConferma(position: Int, capo: Capo) {
        AlertDialog.Builder(requireContext())
            .setTitle("Elimina capo")
            .setMessage("Vuoi davvero eliminare \"${capo.nome}\" dal guardaroba?")
            .setPositiveButton("Elimina") { _, _ ->
                eliminaCapo(position, capo)
            }
            .setNegativeButton("Annulla") { dialog, _ ->
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun eliminaCapo(position: Int, capo: Capo) {
        val currentUser = auth.currentUser ?: return

        capiList.removeAt(position)
        adapter.notifyItemRemoved(position)
        aggiornaVisibilita()

        db.collection("users")
            .document(currentUser.uid)
            .collection("guardaroba")
            .document(capo.id)
            .delete()
            .addOnSuccessListener {
                Snackbar.make(requireView(), "Capo eliminato", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                caricaCapi()
                Snackbar.make(requireView(), "Errore eliminazione: ${it.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun aggiornaVisibilita() {
        if (capiList.isEmpty()) {
            textViewEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            textViewEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
