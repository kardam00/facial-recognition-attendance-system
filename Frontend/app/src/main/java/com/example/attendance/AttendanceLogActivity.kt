package com.example.attendance

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

import com.google.gson.annotations.SerializedName

data class AttendanceEntry(
    val id: Int,
    @SerializedName("emp_id") val empId: String,
    val name: String,
    @SerializedName("date") val date: String,
    @SerializedName("entry_time") val entryTime: String?,
    @SerializedName("exit_time") val exitTime: String?
)


interface AttendanceApi {
    @GET("/attendance")
    fun getLogs(): Call<List<AttendanceEntry>>
}

class AttendanceLogActivity : AppCompatActivity() {

    private lateinit var logTable: TableLayout
    private lateinit var api: AttendanceApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_log)

        logTable = findViewById(R.id.logTable)
        addTableHeader()

        val retrofit = Retrofit.Builder()
            .baseUrl(PrefsHelper.getBaseUrl(this))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(AttendanceApi::class.java)

        api.getLogs().enqueue(object : Callback<List<AttendanceEntry>> {
            override fun onResponse(call: Call<List<AttendanceEntry>>, response: Response<List<AttendanceEntry>>) {
                val logs = response.body()
                if (!logs.isNullOrEmpty())
                {
                    val dates = logs.map { it.date }.distinct().sorted()
                    setupDateSpinner(dates, logs)
                } else {
                    Toast.makeText(this@AttendanceLogActivity, "No logs found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AttendanceEntry>>, t: Throwable) {
                Toast.makeText(this@AttendanceLogActivity, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupDateSpinner(dates: List<String>, allLogs: List<AttendanceEntry>) {
        val spinner = findViewById<Spinner>(R.id.dateSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedDate = dates[position]
                val filteredLogs = allLogs.filter { it.date == selectedDate }

                logTable.removeViews(1, logTable.childCount - 1) // remove rows after header
                filteredLogs.forEach { addRow(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun addTableHeader() {
        val headerRow = TableRow(this)
        val headers = listOf("ID", "Emp ID", "Name", "Date", "Entry Time", "Exit Time")
        headers.forEach { text ->
            val tv = TextView(this)
            tv.text = text
            tv.setPadding(16, 16, 16, 16)
            tv.gravity = Gravity.CENTER
            tv.textSize = 16f
            tv.setBackgroundColor(0xFFCCCCCC.toInt())
            headerRow.addView(tv)
        }
        logTable.addView(headerRow)
    }

    private fun addRow(entry: AttendanceEntry) {
        val row = TableRow(this)
        val values = listOf(
            entry.id.toString(),
            entry.empId,
            entry.name,
            entry.date,
            entry.entryTime ?: "-",
            entry.exitTime ?: "-"
        )
        values.forEach { value ->
            val tv = TextView(this)
            tv.text = value
            tv.setPadding(16, 16, 16, 16)
            tv.gravity = Gravity.CENTER
            tv.textSize = 14f
            row.addView(tv)
        }
        logTable.addView(row)
    }
}
