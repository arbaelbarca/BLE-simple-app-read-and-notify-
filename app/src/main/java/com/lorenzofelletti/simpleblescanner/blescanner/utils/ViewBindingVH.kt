package com.lorenzofelletti.simpleblescanner.blescanner.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class ViewBindingVH constructor(val binding: ViewBinding) :
    RecyclerView.ViewHolder(binding.root) {

    companion object {
        inline fun create(
            parent: ViewGroup,
            crossinline block: (inflater: LayoutInflater, container: ViewGroup, attach: Boolean) -> ViewBinding
        ) = ViewBindingVH(block(LayoutInflater.from(parent.context), parent, false))
    }
}
