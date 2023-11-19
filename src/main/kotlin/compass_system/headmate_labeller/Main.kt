package compass_system.headmate_labeller

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import compass_system.headmate_labeller.extensions.HeadmateLabellerExtension

private val TOKEN = env("TOKEN")

suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
        extensions {
            add(::HeadmateLabellerExtension)
        }
    }

    bot.start()
}
