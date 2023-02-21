package com.example.meongnyang.mypage

import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.meongnyang.R
import com.example.meongnyang.api.RetrofitApi
import com.example.meongnyang.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MypageViewModel: ViewModel() {

    val retrofit = RetrofitApi.create()

    var fbAuth = FirebaseAuth.getInstance()
    var fbFirestore = FirebaseFirestore.getInstance()
    val uid = fbAuth.uid.toString()

    private var username = MutableLiveData<String>()
    val nameData: LiveData<String>
    get() = username

    private var userImg = MutableLiveData<String>()
    val userData: LiveData<String>
    get() = userImg

    private var petImg = MutableLiveData<String>()
    val petdata : LiveData<String>
    get() = petImg

    val dayCount : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val withCount : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val petName : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val petBirth : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val petSex : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val petSpec : MutableLiveData<String> by lazy { MutableLiveData<String>() }

    init {
        viewModelScope.launch {
            fbFirestore!!.collection("users").document(uid).get()
                .addOnSuccessListener { documentsSnapshot ->
                    var id = documentsSnapshot.toObject<Id>()!!
                    Log.d("memberid", id.memberId.toString())

                    retrofit.getMember(id.memberId!!).enqueue(object : Callback<allPet> {
                        override fun onResponse(call: Call<allPet>, response: Response<allPet>) {
                            username.value = response.body()!!.nickname
                            //userImg.value = response.body()!!.memberImg
                            userImg.value = "https://meongnyang.s3.ap-northeast-2.amazonaws.com/feed/%EB%8B%A5%EC%8A%A4%ED%9B%88%ED%8A%B8+%EC%96%B4%EB%8D%9C%ED%8A%B8+%ED%8C%8C%EC%9A%B0%EC%B9%98.jpeg"
                            var conimal = response.body()!!.conimals
                            dayCount.value = conimal[0].ddayadopt.toString()
                            withCount.value = conimal[0].ddayadopt.toString()
                            petName.value = conimal[0].name
                            petSex.value = conimal[0].gender
                            petSpec.value = conimal[0].species
                            //petImg.value = conimal[0].conimalImg
                            petImg.value = "https://meongnyang.s3.ap-northeast-2.amazonaws.com/feed/%EB%8B%A5%EC%8A%A4%ED%9B%88%ED%8A%B8+%EC%96%B4%EB%8D%9C%ED%8A%B8+%ED%8C%8C%EC%9A%B0%EC%B9%98.jpeg"
                        }

                        override fun onFailure(call: Call<allPet>, t: Throwable) {
                            Log.d("mypage", t.message.toString())
                        }
                    })

                    retrofit.getPet(id.conimalId!!).enqueue(object : Callback<PetModel> {
                        override fun onResponse(
                            call: Call<PetModel>,
                            response: Response<PetModel>
                        ) {
                            petBirth.value = response.body()!!.birth
                        }

                        override fun onFailure(call: Call<PetModel>, t: Throwable) {
                            Log.d("mypage", t.message.toString())
                        }
                    })

                }
        }
    }
    fun updateNickname(nickname: String) {
        username.value = nickname
    }
    fun updateUserImg(img: String) {
        userImg.value = img
    }
    fun updatePetImg(img: String) {
        petImg.value = img
    }
}