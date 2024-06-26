package com.nakyung.meongnyang.api

import com.nakyung.meongnyang.App
import com.nakyung.meongnyang.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.lang.reflect.Type

interface RetrofitApi {
    // 회원 API
    @POST("members/findId")
    fun findId(
        @Body email: Email
    ): Call<MemberId>

    @POST("members/login")
    fun loginToken(
        @Body email: Email
    ): Call<LoginUser>

    @POST("members")
    fun userSignUp(
        @Body postUser: PostUser
    ): Call<LoginUser>

    @GET("mypage/{memberId}")
    fun getMember(
        @Path("memberId") memberId: Int
    ): Call<allPet>

    @PATCH("members/updateNickName/{memberId}")
    fun updateNickname(
        @Path("memberId") memberId: Int,
        @Body nickname: Nickname
    ): Call<UserModel>

    @PATCH("members/updatePhoto/{memberId}")
    fun updateProfile(
        @Path("memberId") memberId: Int,
        @Body img: Img
    ): Call<UserModel>

    @DELETE("members/{memberId}")
    fun deleteProfile(
        @Path("memberId") memberId: Int
    ): Call<String>

    // 건강기록부 API
    @POST("records/{memberId}/{conimalId}")
    fun writeDiary(
        @Path ("memberId") memberId: Int,
        @Path ("conimalId") conimalId: Int,
        @Body diary: PostDiary
    ): Call<DiaryModel>

    @FormUrlEncoded
    @PATCH("records/update/{memberId}/{conimalId}")
    fun updateDiary(
        @Path ("memberId", encoded = true) memberId: Int,
        @Path ("conimalId", encoded = true) conimalId: Int,
        @Body diary: PostDiary
    ): Call<DiaryModel>

    @GET("records/{recordId}")
    fun showDiary(
        @Path("recordId") recordId: Int
    ): Call<DiaryModel>

    // 반려동물 API
    @POST("conimals/{memberId}")
    fun enrollPet(
        @Path ("memberId") memberId: Int,
        @Body pet: Pet
    ): Call<PetModel>

    @GET("conimals/{conimalId}")
    fun getPet(
        @Path("conimalId") conimalId: Int
    ): Call<PetModel>

    @GET("conimals/all/{memberId}")
    fun getAllPet(
        @Path("memberId") memberId: Int
    ): Call<List<PetModel>>

    // 커뮤니티 API
    @GET("posts")
    fun findPosts(
    ): Call <List<GetPosts>>

    // postId 반환
    @POST("posts/findid")
    fun findPostId(
        @Body title: Title
    ): Call<PostId>

    // 게시글 작성하기
    @POST("posts/{memberId}")
    fun createPost(
        @Body jsonparams: PostModel,
        @Path("memberId") memberId: Int
    ): Call<GetPosts>
    // 게시글 자세히 보기
    @GET("posts/{postId}")
    fun getPost(
        @Path("postId") postId: Int
    ): Call<GetPosts>

    // 게시글 삭제
    @DELETE("posts/{postId}")
    fun deletePost(
        @Path("postId") postId: Int
    ): Call<okhttp3.ResponseBody>

    // 게시글 수정
    @PUT("posts/{postId}")
    fun editPost(
        @Path("postId") postId: Int,
        @Body jsonparams: EditPostModel
    ): Call<GetPosts>

    // 인기 아가들
    @GET("posts/popular/{typeId}")
    fun getPopularPost(
        @Path("typeId") typeId: Int
    ): Call<GetPosts>

    // 댓글 보기
    @GET("comments/{postId}")
    fun getComments(
        @Path("postId") postId: Int
    ): Call<List<Comment>>

    // commentId 반환
    @POST("comments/findId")
    fun getCommentId(
        @Body contents: Contents
    ): Call<CommentId>

    // 댓글 작성하기
    @POST("comments/{memberId}/{postId}")
    fun writeComment(
        @Path("memberId") memberId: Int,
        @Path("postId") postId: Int,
        @Body contents: String
    ): Call<Comment>

    // 대댓글 작성하기
    @POST("comments/{memberId}/{commentId}/{postId}")
    fun reWriteComment(
        @Path("memberId") memberId: Int,
        @Path("commentId") commentId: Int,
        @Path("postId") postId: Int,
        @Body contents: String
    ): Call<Comment>

    // 좋아요 기능
    @POST("likes/{memberId}/{postId}")
    fun updateLikes(
        @Path("memberId") memberId: Int,
        @Path("postId") postId: Int
    ): Call<Count>

    // 피부병 관련
    @POST("disease")
    fun getDisease(
        @Body name: Name
    ): Call<Result>

    // 사료
    @GET("feed/type/{typeId}")
    fun getFeed(
        @Path("typeId") typeId: Int
    ): Call<List<Feed>>

    @GET("feed/{feedId}")
    fun getByFeedId(
        @Path("feedId") feedId: Int
    ): Call<Feed>

    @GET("feed/efficacy/{efficacyId}")
    fun getByEfficacy(
        @Path("efficacyId") efficacyId: Int
    ): Call<List<Feed>>

    // 수의사 질의응답
    @GET("qna")
    fun getAllQna(
    ): Call<List<Qna>>

    @GET("qna/{qnaId}")
    fun getQna(
        @Path("qnaId") qnaId: Int
    ): Call<QnaModel>

    // 산책지수
    @POST("walk/{category}")
    fun walkScore(
        @Path("category") category: Int,
        @Body location: Walk
    ): Call<Score>

    companion object {
        private const val BASE_URL = "http://43.201.122.215:8080/"

        fun create(): RetrofitApi {
            val gson: Gson = GsonBuilder().setLenient().create()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(NullOnEmptyConverterFactory())
                .build()
                .create(RetrofitApi::class.java)
        }

        private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor()).build()


        class AuthInterceptor: Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response = with(chain) {
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", App.prefs.getString("token", "no_token"))
                    .build()

                return chain.proceed(newRequest)
            }
        }

        class NullOnEmptyConverterFactory: Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<out Annotation>,
                retrofit: Retrofit
            ): Converter<ResponseBody, *>? {
                val delegate: Converter<ResponseBody, *> =
                    retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
                return Converter { body: ResponseBody ->
                    if (body.contentLength() == 0L) return@Converter null
                    delegate.convert(body)
                }
            }
        }
    }

}