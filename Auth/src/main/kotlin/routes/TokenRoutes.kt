package app.aviaslaves.auth.routes


import app.aviaslaves.auth.controllers.refreshTokens
import app.aviaslaves.auth.controllers.verifyAccessToken
import app.aviaslaves.auth.controllers.verifyRefreshTokenAndGetData
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.tokenRoutes() {
    route("/token") {
        post("/data") {
            call.verifyRefreshTokenAndGetData()
        }
        post("/verify") {
            call.verifyAccessToken()
        }
        post("/refresh") {
            call.refreshTokens()
        }
    }
}