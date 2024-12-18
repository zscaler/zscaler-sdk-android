package com.zscaler.sdk.demoapp.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zscaler.sdk.demoapp.databinding.ActivityLoggerViewBinding
import java.io.BufferedReader
import java.io.File

class EventLogViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoggerViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoggerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val csvData = readCSVFromInternalStorage()
        binding.textViewCsvContent.text = csvData.joinToString("\n\n")
    }

    private fun readCSVFromInternalStorage(): List<String> {
        val csvFile = File(filesDir.absolutePath + "/zdk/events.csv")
        val content = mutableListOf<String>()

        if (csvFile.exists()) {
            val bufferedReader = BufferedReader(csvFile.reader())
            bufferedReader.forEachLine { line ->
                content.add(line)
            }
            bufferedReader.close()
        } else {
            content.add("File not found!")
        }
        return content
    }
}
