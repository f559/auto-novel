package api

import data.User
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

data class JwtUser(
    val username: String,
    val role: User.Role,
) {
    fun atLeastMaintainer(): Boolean {
        return role == User.Role.Maintainer || role == User.Role.Admin
    }
}

private fun JWTPrincipal.toJwtUser(): JwtUser = payload.let { payload ->
    val username = payload.getClaim("username").asString()
    val role = payload.getClaim("role")
        .takeIf { !it.isNull }
        ?.let { User.Role.valueOf(it.asString()) }
        ?: User.Role.Normal
    JwtUser(username, role)
}

fun ApplicationCall.jwtUser(): JwtUser =
    principal<JWTPrincipal>()!!.toJwtUser()

fun ApplicationCall.jwtUserOrNull(): JwtUser? =
    principal<JWTPrincipal>()?.toJwtUser()

inline fun <T> ApplicationCall.requireAtLeastMaintainer(block: () -> Result<T>) =
    if (jwtUser().atLeastMaintainer()) {
        block()
    } else {
        httpUnauthorized("只有维护者及以上才有权限执行此操作")
    }
