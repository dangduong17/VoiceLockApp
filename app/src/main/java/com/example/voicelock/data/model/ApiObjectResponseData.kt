package com.example.voicelock.data.model

import com.google.gson.annotations.SerializedName

data class ApiObjectResponseData<T>(
    @SerializedName("message")
    var message: String,
    @SerializedName("data")
    var dataResponse: T,
    @SerializedName("status")
    var status: Int,
    @SerializedName("paging")
    var paging: ApiPagingInfo?,
)

data class ApiPagingInfo(
    @SerializedName("current_page")
    var currentPage: Int,
    @SerializedName("page_size")
    var pageSize: Int,
    @SerializedName("total")
    var total: Int,
    @SerializedName("total_pages")
    var totalPages: Int,
)
