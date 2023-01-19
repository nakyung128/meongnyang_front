package com.example.meongnyang.mypage

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import com.example.meongnyang.R
import com.example.meongnyang.databinding.MypageActivityAddBinding
import com.example.meongnyang.databinding.SkinActivityCameraBinding
import java.util.Calendar

class AddActivity : AppCompatActivity() {
    private lateinit var binding: MypageActivityAddBinding
    var birthString = ""
    var adoptString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MypageActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.birthDay.setOnClickListener {
            val calender = Calendar.getInstance()

            val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                birthString = "${year}년 ${month+1}월 ${dayOfMonth}일"
                binding.birthDay.text = birthString
            }
            // 오늘 날짜로 지정해 놓고 다이얼로그 띄우기
            DatePickerDialog(this, dateSetListener, calender.get(Calendar.YEAR), calender.get(Calendar.MONTH), calender.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.adoptDay.setOnClickListener {
            val calender = Calendar.getInstance()

            val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                adoptString = "${year}년 ${month+1}월 ${dayOfMonth}일"
                binding.adoptDay.text = adoptString
            }
            // 오늘 날짜로 지정해 놓고 다이얼로그 띄우기
            DatePickerDialog(this, dateSetListener, calender.get(Calendar.YEAR), calender.get(Calendar.MONTH), calender.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 스피너 설정
        val speciesAdapter = ArrayAdapter.createFromResource(this, R.array.species_array, android.R.layout.simple_spinner_dropdown_item)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.selectSpecies.adapter = speciesAdapter // 어댑터와 연결

        binding.selectSpecies.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                // 선택하세요 이외의 것을 클릭했을 때만 기능 구현되게
                if (!binding.selectSpecies.getItemAtPosition(position).equals("선택하세요")) {

                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }
}