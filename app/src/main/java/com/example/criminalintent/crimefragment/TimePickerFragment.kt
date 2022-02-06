package com.example.criminalintent.crimefragment

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_TIME = "time"

class TimePickerFragment : DialogFragment() {

    interface Callbacks {
        fun onTimeSelected(time: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(ARG_TIME) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val calendarYear = calendar.get(Calendar.YEAR)
        val calendarMonth = calendar.get(Calendar.MONTH)
        val calendarDay = calendar.get(Calendar.DAY_OF_MONTH)
        val timeListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            val resultTime: Date = GregorianCalendar(calendarYear, calendarMonth, calendarDay, hour, minute).time

            targetFragment?.let { fragment ->
                (fragment as Callbacks).onTimeSelected(resultTime)
            }
        }

        val initialHour = calendar.get(Calendar.HOUR)
        val initialMinute = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHour,
            initialMinute,
            false
        )
    }

    companion object {
        fun newInstance(time: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_TIME, time)
            }
            return TimePickerFragment().apply { arguments = args }
        }
    }
}