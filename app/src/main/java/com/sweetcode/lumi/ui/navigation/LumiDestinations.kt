package com.sweetcode.lumi.ui.navigation

sealed class LumiDestination(val route: String) {
    object Onboarding : LumiDestination("onboarding")
    object Library : LumiDestination("library")
    object Settings : LumiDestination("settings")

    object CollectionDetail : LumiDestination("collection/{collectionName}") {
        fun build(name: String) = "collection/$name"
    }

    object Reader : LumiDestination("reader/{itemId}") {
        fun build(id: String) = "reader/$id"
    }

    object ItemDetail : LumiDestination("item/{itemId}") {
        fun build(id: String) = "item/$id"
    }
}