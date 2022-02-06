package com.example.criminalintent.crimefragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.example.criminalintent.R
import com.example.criminalintent.utils.getScaledBitmap
import java.io.File

private const val ARG_PHOTO = "photoView"

class FullPhotoDialogFragment : DialogFragment() {
    private lateinit var photoView: ImageView
    private var photoFileName: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoFileName = arguments?.getSerializable(ARG_PHOTO) as File
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_full_photo, container, false)
        photoView = view.findViewById(R.id.photo_view) as ImageView
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (photoFileName != null) photoView.setImageBitmap(
            getScaledBitmap(
                photoFileName!!.path,
                requireActivity()
            )
        )
    }

    companion object {
        fun newInstance(photoFileName: File): FullPhotoDialogFragment {
            val args = Bundle().apply {
                putSerializable(ARG_PHOTO, photoFileName)
            }
            return FullPhotoDialogFragment().apply { arguments = args }
        }
    }
}