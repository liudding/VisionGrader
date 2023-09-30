package com.linkstar.visiongrader.ui.scanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.linkstar.visiongrader.databinding.FragmentPickerBinding
import com.linkstar.visiongrader.databinding.FragmentPickerItemBinding

const val ARG_ITEMS = "item_count"

class PickerFragment : DialogFragment() {
    private var title: String = ""
    private var items: List<Item> = emptyList()

    private var _binding: FragmentPickerBinding? = null
    private val binding get() = _binding!!

    private var mOnItemSelectedListener: OnItemSelectedListener? = null

    fun setOnItemSelectedListener(onItemSelectedListener: OnItemSelectedListener) {
        mOnItemSelectedListener = onItemSelectedListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPickerBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.pickerTitle.text = title

        binding.recyclerView.layoutManager =  LinearLayoutManager(context)
        binding.recyclerView.adapter = ItemAdapter(items.size)
    }

    private inner class ViewHolder internal constructor(binding: FragmentPickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val text: TextView = binding.text
    }

    private inner class ItemAdapter internal constructor(private val mItemCount: Int) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                FragmentPickerItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = items[position].label
            holder.itemView.setOnClickListener{
                mOnItemSelectedListener?.onItemSelectedListener(position)
                dismiss()
            }
        }

        override fun getItemCount(): Int {
            return mItemCount
        }
    }

     data class Item (
         val label: String,
         val value: String
    )

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PickerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(items: List<Item>, title: String) =
            PickerFragment().apply {
                this.items = items
                this.title = title
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    interface OnItemSelectedListener {
        fun onItemSelectedListener(position: Int)
    }
}