package app.aviaslaves.auth.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import app.aviaslaves.auth.controllers.loginUser
import app.aviaslaves.auth.controllers.signupUser


fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            call.loginUser()
        }
        post("/signup") {
            call.signupUser()
        }
    }
}
