package com.example.criminalintent.crimelist

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.criminalintent.R
import com.example.criminalintent.model.Crime
import java.util.*

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {

    // Позволяет передавать события кликов из CrimeListFragment обратно на хост-активити.
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private var callbacks: Callbacks? = null
    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeAdapter? = CrimeAdapter()

    private val crimeListViewModel: CrimeListViewModel by lazy {
        val factory = CrimeListViewModel.ViewModelFactory()
        ViewModelProvider(this, factory).get(CrimeListViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    // Сообщаем FragmentManager, что экземпляр CrimeListFragment должен получать callbacks меню
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            { crimes ->
                crimes?.let {
                    Log.d(TAG, "Got crimes ${it.size}")
                    updateUI(it)
                }
            }
        )
    }

    private fun updateUI(crimes: List<Crime>) {
        adapter?.submitList(crimes)
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private abstract class CrimeHolder(view: View) : RecyclerView.ViewHolder(view) {
        var crime = Crime()
        val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
    }

    private inner class NormalCrimeHolder(view: View) : CrimeHolder(view), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = DateFormat.format("EEEE, MMM dd, yyyy", this.crime.date)
            solvedImageView.visibility = if (crime.isSolved) View.VISIBLE else View.GONE
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crimeId = crime.id)
        }
    }

    private inner class SpecialCrimeHolder(view: View) : CrimeHolder(view), View.OnClickListener {
        val contactThePolice: Button = itemView.findViewById(R.id.contact_police_button)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = this.crime.date.toString()
            contactThePolice.setOnClickListener {
                Toast.makeText(context, "${crime.title} contact the police!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crimeId = crime.id)
        }
    }

    private inner class CrimeAdapter :
        ListAdapter<Crime, CrimeHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            return if (viewType == 2) {
                val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
                NormalCrimeHolder(view)
            } else {
                val view = layoutInflater.inflate(R.layout.list_item_crime_special, parent, false)
                SpecialCrimeHolder(view)
            }
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = currentList[position]
            when (holder) {
                is NormalCrimeHolder -> holder.bind(crime)
                is SpecialCrimeHolder -> holder.bind(crime)
                else -> throw IllegalArgumentException()
            }
        }

        override fun getItemCount() = currentList.size

        override fun getItemViewType(position: Int): Int {
            /*return when (crimes[position].requiresPolice) {
                true -> 1
                else -> 2
            }*/
            return 2
        }

    }

    object DiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}