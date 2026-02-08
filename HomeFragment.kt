package com.example.progettopm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progettopm.databinding.HomeFragmentBinding

class HomeFragment : Fragment() {

    private lateinit var binding: HomeFragmentBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var randomAdapter: CapoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.home_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        randomAdapter = CapoAdapter(listOf())
        binding.randomCapiRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.randomCapiRecyclerView.adapter = randomAdapter

        // Osserva i capi random
        viewModel.capiRandom.observe(viewLifecycleOwner, Observer { listaCapi ->
            randomAdapter.updateData(listaCapi)

            // 🔹 NUOVA LOGICA: Gestione vista vuota
            if (listaCapi.isEmpty()) {
                // Se la lista è vuota: Nascondi la lista, Mostra la scritta
                binding.randomCapiRecyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            } else {
                // Se c'è qualcosa: Mostra la lista, Nascondi la scritta
                binding.randomCapiRecyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            }
        })

        // Carica dashboard e capi random
        viewModel.caricaDashboard()
        viewModel.caricaCapiRandom()

        // Benvenuto
        viewModel.username.observe(viewLifecycleOwner) { username ->
            binding.welcomeTextView.text = "Ciao $username"
        }

    }
}
