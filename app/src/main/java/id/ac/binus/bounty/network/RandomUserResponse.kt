package id.ac.binus.bounty.network

import id.ac.binus.bounty.models.User
import id.ac.binus.bounty.utils.SessionManager
import java.net.URI
import java.net.URISyntaxException

class RandomUserResponse {
    @JvmField var results: List<Result>? = null
    @JvmField var error: String? = null

    class Result {
        @JvmField var name: Name? = null
        @JvmField var email: String? = null
        @JvmField var picture: Picture? = null
        @JvmField var login: Login? = null
    }

    class Name {
        @JvmField var first: String? = null
        @JvmField var last: String? = null
    }

    class Picture { @JvmField var large: String? = null }
    class Login { @JvmField var uuid: String? = null }

    fun toUsers(): List<User> {
        if (error != null) return emptyList()
        val users = mutableListOf<User>()
        for (result in results ?: return users) {
            val login = result.login ?: continue
            val nameData = result.name ?: continue
            val id = text(login.uuid)
            val name = "${text(nameData.first)} ${text(nameData.last)}".trim()
            val email = text(result.email)
            if (id.isEmpty() || name.isEmpty() || !SessionManager.isValidEmail(email)) continue
            var avatar = text(result.picture?.large)
            try {
                val uri = URI(avatar)
                if (!uri.scheme.equals("https", true) || uri.host == null) avatar = ""
            } catch (_: URISyntaxException) {
                avatar = ""
            }
            users += User(id, name, email, avatar, 0.0)
            if (users.size == 10) break
        }
        return users
    }

    private fun text(value: String?) = value?.trim().orEmpty()
}
