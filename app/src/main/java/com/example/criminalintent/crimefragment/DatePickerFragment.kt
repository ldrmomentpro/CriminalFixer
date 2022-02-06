package com.example.criminalintent.crimefragment

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_DATE = "date"

class DatePickerFragment : DialogFragment() {


    // Интерфейс для отправки даты в CrimeFragment
    interface Callbacks {
        fun onDateSelected(date: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Слушатель который будет отправлять дату в CrimeFragment
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            // Выбранная дата будет в формате год, месяц, день, но для отправки необходим объект Date
            // с помощью GregorianCalendar().time получаем  объект Date
            val resultDate: Date = GregorianCalendar(year, month, day).time

            targetFragment?.let { fragment ->
                // Экземпляр фрагмента передается в интерфейс Callbacks и передается новая дата
                (fragment as Callbacks).onDateSelected(resultDate)
            }
        }

        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(
            requireContext(),
            dateListener,
            initialYear,
            initialMonth,
            initialDay
        )
    }

    companion object {
        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
            return DatePickerFragment().apply { arguments = args }
        }
    }
}