package com.alexstephanov.wondersoftheworld.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.alexstephanov.wondersoftheworld.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_detailed.*
import kotlinx.android.synthetic.main.fragment_detailed.linear_layout_detailed
import kotlinx.android.synthetic.main.fragment_nature_wonders_detailed.*

class NatureWondersDetailedFragment(private var listener: OnFragmentEventListener? = null) : Fragment() {

    private var id: Int? = null
    private var name: String? = null
    private var description: String? = null
    private var type: String? = null
    private var location: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var url: String? = null
    private lateinit var thumbnail: ImageView

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = context as OnFragmentEventListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments?.getInt("id") ?: 0
        name = arguments?.getString("name") ?: ""
        description = arguments?.getString("description") ?: ""
        type = arguments?.getString("type") ?: ""
        location = arguments?.getString("location") ?: ""
        latitude = arguments?.getDouble("latitude") ?: 0.0
        longitude = arguments?.getDouble("longitude") ?: 0.0
        url = arguments?.getString("url") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nature_wonders_detailed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        thumbnail = view.findViewById(R.id.thumbnail_nature_wonders_detailed)
        Picasso.get().load(url).into(thumbnail)

        name_nature_wonders_detailed.text = name
        type_nature_wonders_detailed.text = type
        location_nature_wonders_detailed.text = location
        description_nature_wonders_detailed.text = description

        linear_layout_nature_wonders_detailed.setOnClickListener{
            listener?.onBackgroundClickEvent()
        }
    }

    interface OnFragmentEventListener {
        fun onBackgroundClickEvent()
    }

}