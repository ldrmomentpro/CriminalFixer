package com.example.criminalintent.crimefragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.R
import com.example.criminalintent.model.Crime
import com.example.criminalintent.utils.getScaledBitmap
import java.io.File
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 1
private const val REQUEST_CONTACT = 2
private const val REQUEST_PHOTO = 3
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val DIALOG_PHOTO = "DialogPhoto"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {

    // Отражает правки, которые пользователь делает в данный момент
    private lateinit var crime: Crime

    // Сохранение местонахождения файла фото, которое пригодится в нескольких местах
    private lateinit var photoFile: File

    // Uri будет указывать на место где должно зраниться фото в файловой системе
    private lateinit var photoUri: Uri

    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        val factory = CrimeDetailViewModel.ViewModelFactory()
        ViewModelProvider(this, factory).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        // Загрузка данных из БД
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        timeButton = view.findViewById(R.id.crime_time) as Button
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Данные в том виде, в котором они в наст. время хранятся в БД
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)

                /**
                 * getUriForFile() преобразует локальный путь к файлу в Uri, который видит
                 * приложение камеры.
                 */
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.example.criminalintent.fileprovider", photoFile
                )
                updateUI()
            }
        }
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        val crimeDate = DateFormat.format("EEEE, d MMM, yyyy", this.crime.date)
        dateButton.text = crimeDate
        val crimeTime = DateFormat.format("hh:mm a", this.crime.date)
        timeButton.text = crimeTime
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            // Пропуск анимации отображения установленного флажка, когда программно устанавливается флажок
            jumpDrawablesToCurrentState()
        }

        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }

        updatePhotoView()
    }

    private var imageViewWidth = 0
    private var imageViewHeight = 0

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, imageViewWidth, imageViewHeight)
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri = data.data!!
                // Указать, для каких полей ваш запрос должен возвращать значения.
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // Выполняемый здесь запрос - contactUri похож на предложение "where"
                val cursor = requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }

                    // Первый столбец первой строки данных - это имя вашего подозреваемого.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val df = DateFormat.getBestDateTimePattern(Locale.getDefault(), DATE_FORMAT)
        val dateString = DateFormat.format(df, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                crime.title = p0.toString()
            }

            override fun afterTextChanged(p0: Editable?) {}
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                val packageManager: PackageManager = requireActivity().packageManager
                val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(
                    pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                if (resolvedActivity == null) {
                    isEnabled = false
                }
            }
        }
        photoButton.apply {
            // Ищет приложение камеру, если не находит - блокирует кнопку
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(
                captureImage, PackageManager.MATCH_DEFAULT_ONLY
            )
            if (resolvedActivity == null) isEnabled = false

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> = packageManager
                    .queryIntentActivities(
                        captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                // Даем разрешение на запись для каждого активити который может обрабатывать cameraImage
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }

        photoView.apply {
            viewTreeObserver.addOnGlobalLayoutListener {
                imageViewWidth = width
                imageViewHeight = height
            }
            setOnClickListener {
                if (photoFile.exists()) {
                    FullPhotoDialogFragment.newInstance(photoFile).apply {
                        show(this@CrimeFragment.parentFragmentManager, DIALOG_PHOTO)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply { arguments = args }
        }
    }

    override fun onTimeSelected(time: Date) {
        crime.date = time
        updateUI()
    }
}