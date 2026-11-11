package com.app.feature.applications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.core.common.UiState
import com.app.core.common.messageRes
import com.app.core.model.ApplicationStatus
import com.app.designsystem.labelRes
import com.app.feature.applications.databinding.FragmentApplicationsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

interface ApplicationNavigation { fun openApplication(id: String) }

@AndroidEntryPoint
class ApplicationsFragment : Fragment() {
    private val viewModel: ApplicationsViewModel by viewModels()
    private var binding: FragmentApplicationsBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return FragmentApplicationsBinding.inflate(inflater, container, false).also { binding = it }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = requireNotNull(binding)
        val rows = ApplicationsAdapter { id -> (requireActivity() as ApplicationNavigation).openApplication(id) }
        views.applications.layoutManager = LinearLayoutManager(requireContext())
        views.applications.adapter = rows
        views.search.setText(viewModel.currentFilter.query)
        views.search.doAfterTextChanged { viewModel.search(it.toString()) }
        val options = listOf<ApplicationStatus?>(null) + ApplicationStatus.entries
        views.status.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            options.map { it?.let { status -> getString(status.labelRes()) } ?: getString(R.string.all_statuses) })
        views.status.setSelection(options.indexOf(viewModel.currentFilter.status))
        views.status.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) { viewModel.filter(options[position]) }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        views.retry.setOnClickListener { viewModel.retry() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    views.loading.isVisible = state is UiState.Loading
                    views.retry.isVisible = state is UiState.Error
                    views.message.isVisible = state is UiState.Error || (state is UiState.Success && state.data.isEmpty())
                    views.message.text = when (state) {
                        is UiState.Error -> getString(state.failure.messageRes())
                        else -> getString(R.string.empty_applications)
                    }
                    rows.submitList((state as? UiState.Success)?.data ?: emptyList())
                }
            }
        }
    }
    override fun onDestroyView() {
        binding?.applications?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
