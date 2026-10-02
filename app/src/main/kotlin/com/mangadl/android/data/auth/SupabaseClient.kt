package com.mangadl.android.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    // Replace these with your actual Supabase project URL and anon key
    // Set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties
    const val SUPABASE_URL = "https://YOUR_PROJECT.supabase.co"
    const val SUPABASE_ANON_KEY = "YOUR_ANON_KEY"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
