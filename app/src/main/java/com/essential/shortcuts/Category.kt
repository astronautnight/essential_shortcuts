package com.essential.shortcuts // Change this to YOUR package name

data class Category(
    val id: String,
    val name: String,
    val colorRes: Int,  // Color resource ID
    val tools: List<Tool>
)
