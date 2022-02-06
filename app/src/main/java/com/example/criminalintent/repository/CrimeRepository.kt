package com.example.criminalintent.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.criminalintent.database.CrimeDatabase
import com.example.criminalintent.database.migration_1_2
import com.example.criminalintent.model.Crime
import java.io.File
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "crime-database"

// класс Singleton
// Конструктор можно пометить как приватный, чтобы убедиться в отсутствии компонентов
// которые могут пойти против системы и создать свой собственный экземпляр
class CrimeRepository private constructor(context: Context) {

    /**
     * Теперь нужно добавить два свойства, чтобы он умел хранить ссылки на базу данных и объекты ДАО
     */

    /**
     * Room.databaseBuilder() создает конкретную реализацию вашего абстрактного класса CrimeDatabase
     * с использованием трех параметров:
     * 1) нужен context, так как БД обращается к файловой системе
     * 2) класс БД, которую Room должен создать
     * 3) имя файла БД, которую создаст Room
     */
    // Ссылка на БД
    private val database: CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(migration_1_2).build()

    // Ссылка на ДАО
    private val crimeDao = database.crimeDao()

    private val executor = Executors.newSingleThreadExecutor()
    //Определение местонахождения файла фотографии
    private val filesDir = context.applicationContext.filesDir

    /**
     * Добавление функций репозитория
     */
    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()
    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    // Функции с использование Executors, для выполнения блока в потоке
    fun updateCrime(crime: Crime) {
        executor.execute { crimeDao.updateCrime(crime) }
    }
    fun addCrime(crime: Crime) {
        executor.execute { crimeDao.addCrime(crime) }
    }

    // Возвращает объекты File, указывающие в нужные места
    fun getPhotoFile(crime: Crime): File = File(filesDir, crime.photoFileName)

    companion object {
        private var INSTANCE: CrimeRepository? = null

        // Инициализирует новый экземпляр в репозиторий
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        // Обеспечивает доступ к экземпляру
        fun get(): CrimeRepository {
            return INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}