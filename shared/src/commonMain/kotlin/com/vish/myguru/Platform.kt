package com.vish.myguru

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform