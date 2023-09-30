package com.linkstar.visiongrader.ui.scanner

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jiangdg.ausbc.utils.ToastUtils
import com.linkstar.visiongrader.common.extensions.toBitmap
import com.linkstar.visiongrader.data.SUBJECTS
import com.linkstar.visiongrader.data.model.Textbook
import com.linkstar.visiongrader.data.model.Workbook
import com.linkstar.visiongrader.utils.Api
import com.linkstar.visiongrader.utils.Utils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant

class ScannerViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    val subjects: List<SUBJECTS> = SUBJECTS.values().toList()

    private val _textbooks = MutableLiveData<List<Textbook>>()
    val textbooksState: LiveData<List<Textbook>> = _textbooks

    private val _workbooks = MutableLiveData<List<Workbook>>()
    val workbooksState: LiveData<List<Workbook>> = _workbooks


    private val _subject = MutableLiveData<SUBJECTS?>()
    val subjectState: LiveData<SUBJECTS?> = _subject

    private val _textbook = MutableLiveData<Textbook?>()
    val textbookState: LiveData<Textbook?> = _textbook

    private val _workbook = MutableLiveData<Workbook?>()
    val workbookState: LiveData<Workbook?> = _workbook


    private val _currentStudent = MutableLiveData<String?>()
    val currentStudentState: LiveData<String?> = _currentStudent
    var prevScannedStudent: String? = null


    private val _scannedList = MutableLiveData<List<String>>().apply {
        value = mutableListOf()
    }

    val scannedList: LiveData<List<String>> = _scannedList



    @OptIn(DelicateCoroutinesApi::class)
    fun getTextbooks(subject: String) {
        GlobalScope.launch {
            Api.getTextbooks(subject).let {
                Log.d("SVM", it.toString())
                _textbooks.postValue(it)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getWorkbooks(subject: String, textbook: String) {
        GlobalScope.launch {
            Api.getWorkbooks(subject, textbook).let {
                _workbooks.postValue(it)
            }
        }
    }

    fun subjectChanged(sub: String) {
        val subject = subjects.find { it.code == sub }
        _subject.value = subject
        _textbook.value = null
        _workbook.value = null

        getTextbooks(sub)
    }

    fun textbookChanged(id: String) {
        if (id == (_textbook.value?.id ?: "")) {
            return
        }

        val tb = _textbooks.value?.find { it.id == id }
        _textbook.value = tb
        _workbook.value = null

        _subject.value?.let { getWorkbooks(it.code, id) }
    }

    fun workbookChanged(id: String) {
        if (id == (_workbook.value?.id ?: "")) {
            return
        }

        val b = _workbooks.value?.find { it.id == id }
        _workbook.value = b
    }

    fun addNewScanned(student: String, images: ScannedImages) {
//        if (_scannedList.value?.contains(student) == true) {
//            return
//        }

        _scannedList.postValue(listOf(student) + _scannedList.value!!)

        val scansDir = Utils.getExternalFilesDir("scans")
        val studentDir = Utils.joinPath(scansDir, "quiz", student)
        val time = "${Instant.now().toEpochMilli()}"

        Utils.ensurePathExists(studentDir)

        Utils.saveImage(images.origin, Utils.joinPath(studentDir, "${time}-origin.png"))
        Utils.saveImage(images.cropped, Utils.joinPath(studentDir, "${time}-cropped.png"))

        if (images.twoPage) {
            Utils.saveImage(images.left!!, Utils.joinPath(studentDir, "${time}-left.png"))
            Utils.saveImage(images.right!!, Utils.joinPath(studentDir, "${time}-right.png"))
        }

        prevScannedStudent = student
    }

    fun addNewScanned(images: ScannedImages) {
        _currentStudent.value?.let {
            addNewScanned(it, images)
        }
    }


    fun setCurrentStudent(code: String?) {
        if (_currentStudent.value != code) {
//            prevScannedStudent = _currentStudent.value
            _currentStudent.postValue(code)
        }
    }

    fun isDuplicateScanned(): Boolean {
        return _currentStudent.value != null && prevScannedStudent == _currentStudent.value
    }

    fun isScannedValid(): Boolean {
        return _currentStudent.value != null
    }

}