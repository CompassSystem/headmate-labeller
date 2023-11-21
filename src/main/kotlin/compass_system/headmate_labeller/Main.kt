package compass_system.headmate_labeller

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import compass_system.headmate_labeller.extensions.HeadmateLabellerExtension

private val TOKEN = env("DISCORD_TOKEN")
private val TEST_GUILD = envOrNull("TEST_GUILD")
private val PLURALKIT_TOKEN = env("PLURALKIT_TOKEN")

suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
		applicationCommands {
			TEST_GUILD?.let { defaultGuild(it) }
		}

        extensions {
            add { HeadmateLabellerExtension(PLURALKIT_TOKEN) }
        }
    }

    bot.start()
}
