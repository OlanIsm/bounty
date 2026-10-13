package id.ac.binus.bounty.models

class User(
    @JvmField val id: String,
    @JvmField val name: String,
    @JvmField val email: String,
    @JvmField val avatarUrl: String,
    @JvmField val demoBalance: Double
)
