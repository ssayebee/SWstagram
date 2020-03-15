package com.example.hagome.howlstargram.model

data class FollowDTO(
        //현재 내 계정을 follow 하는 사람
        var followerCount: Int = 0,
        var followers: MutableMap<String, Boolean> = HashMap(),

        //내가 follow 하는 사람
        var followingCount: Int = 0,
        var followings : MutableMap<String, Boolean> = HashMap()
)