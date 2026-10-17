package com.essential.shortcuts  // Change this to YOUR package name

data class Tool(
    val id: String,
    val name: String,
    val icon: Int,  // Drawable resource ID
    val categoryId: String,
    var lastUsedTimestamp: Long = 0L
)
