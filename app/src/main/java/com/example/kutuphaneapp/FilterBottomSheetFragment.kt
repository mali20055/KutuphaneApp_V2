package com.example.kutuphaneapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import com.example.kutuphaneapp.model.FilterState
import com.example.kutuphaneapp.viewmodel.LibraryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: LibraryViewModel by viewModels({ requireParentFragment() })
    private val selectedTags = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<TextInputEditText>(R.id.et_filter_title)
        val etAuthor = view.findViewById<TextInputEditText>(R.id.et_filter_author)
        val etPublisher = view.findViewById<TextInputEditText>(R.id.et_filter_publisher)
        val etMinPages = view.findViewById<TextInputEditText>(R.id.et_min_pages)
        val etMaxPages = view.findViewById<TextInputEditText>(R.id.et_max_pages)
        val rgStatus = view.findViewById<RadioGroup>(R.id.rg_status)
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar_filter)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_tags_filter)
        val btnApply = view.findViewById<Button>(R.id.btn_apply_filter)
        val btnClear = view.findViewById<Button>(R.id.btn_clear_filter)

        // Mevcut filtre durumunu yükle
        val currentState = viewModel.filterState.value ?: FilterState()
        etTitle.setText(currentState.titleQuery)
        etAuthor.setText(currentState.authorQuery)
        etPublisher.setText(currentState.publisherQuery)
        etMinPages.setText(currentState.minPageCount?.toString() ?: "")
        etMaxPages.setText(currentState.maxPageCount?.toString() ?: "")
        ratingBar.rating = currentState.minRating.toFloat()
        selectedTags.addAll(currentState.selectedTags)

        when (currentState.selectedStatus) {
            "reading" -> rgStatus.check(R.id.rb_reading)
            "to_read" -> rgStatus.check(R.id.rb_to_read)
            "read" -> rgStatus.check(R.id.rb_read)
            else -> rgStatus.check(R.id.rb_all)
        }

        // Dinamik Etiketleri Gözlemle ve Doldur
        viewModel.availableTags.observe(viewLifecycleOwner) { tags ->
            chipGroup.removeAllViews()
            tags.forEach { tagName ->
                val chip = Chip(requireContext())
                chip.text = tagName
                chip.isCheckable = true
                chip.isChecked = selectedTags.contains(tagName)
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!selectedTags.contains(tagName)) selectedTags.add(tagName)
                    } else {
                        selectedTags.remove(tagName)
                    }
                }
                chipGroup.addView(chip)
            }
        }

        btnApply.setOnClickListener {
            val status = when (rgStatus.checkedRadioButtonId) {
                R.id.rb_reading -> "reading"
                R.id.rb_to_read -> "to_read"
                R.id.rb_read -> "read"
                else -> null
            }

            val newState = FilterState(
                titleQuery = etTitle.text.toString().trim(),
                authorQuery = etAuthor.text.toString().trim(),
                publisherQuery = etPublisher.text.toString().trim(),
                minPageCount = etMinPages.text.toString().toIntOrNull(),
                maxPageCount = etMaxPages.text.toString().toIntOrNull(),
                minRating = ratingBar.rating.toInt(),
                selectedStatus = status,
                selectedTags = selectedTags.toList()
            )
            viewModel.updateFilter(newState)
            dismiss()
        }

        btnClear.setOnClickListener {
            viewModel.updateFilter(FilterState())
            dismiss()
        }
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
    }
}
