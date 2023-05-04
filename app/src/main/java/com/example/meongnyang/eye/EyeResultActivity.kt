package com.example.meongnyang.eye

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import com.example.meongnyang.NaviActivity
import com.example.meongnyang.R
import com.example.meongnyang.api.RetrofitApi
import com.example.meongnyang.databinding.EyeActivityResultBinding
import com.example.meongnyang.databinding.FragmentHealthBinding
import com.example.meongnyang.databinding.SkinActivityResultBinding
import com.example.meongnyang.feed.FeedFragment
import com.example.meongnyang.home.HomeFragment
import com.example.meongnyang.map.MapActivity
import com.example.meongnyang.model.Name
import com.example.meongnyang.model.Result
import com.example.meongnyang.qna.QnaFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

class EyeResultActivity : AppCompatActivity() {
    private lateinit var binding: EyeActivityResultBinding
    var result = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.eye_activity_result)

        // 이미지 띄우기
        val intent = intent
        val bytes = intent.getByteArrayExtra("image")
        result = intent.getStringExtra("result").toString()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size)
        binding.eyeResultImg.setImageBitmap(bitmap)

        Log.d("pet", result)

        val retrofit = RetrofitApi.create()

        var result = Name(result)

        retrofit.getDisease(result).enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                if (response.isSuccessful) {
                    binding.eyeResultTitle.text = "안구 질환이 있는 것 같아요"
                    binding.eyeResultName.text = "⚠️ ${response.body()!!.name}(으)로 의심돼요 ⚠️"
                    binding.reasonTextEye.text = response.body()!!.reason
                    binding.manageTextEye.text = response.body()!!.manage
                }
            }
            override fun onFailure(call: Call<Result>, t: Throwable) {
                Log.d("error", t.message.toString())

            }
        })

        // 메뉴 클릭 시 이동
        binding.goMainBtn.setOnClickListener {
            val intent = Intent(this, NaviActivity::class.java)
            startActivity(intent)
        }
        binding.goMapBtn.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
        binding.goQnaBtn.setOnClickListener {
            val intent = Intent(this, NaviActivity::class.java)
            intent.putExtra("fragment", "qna")
            startActivity(intent)
        }
    }
}