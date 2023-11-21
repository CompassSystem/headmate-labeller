package compass_system.headmate_labeller.extensions

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import compass_system.headmate_labeller.PkSystem
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.Json
import java.lang.RuntimeException

class HeadmateLabellerExtension : Extension() {
	override val name = "headmate-labeller"
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "headmate-labeller"
			description = "Headmate labeller commands."

			check { hasPermission(Permission.Administrator) }

			ephemeralSubCommand(::CreateListArgs) {
				name = "list"
				description = "Create or update a list of headmates."

				action { upsertHeadmateList() }
			}

			ephemeralSubCommand(::CreateListArgs) {
				name = "purge"
				description = "Delete headmate embeds missing from export."

				action { purgeHeadmateList() }
			}
		}
	}

	private suspend fun EphemeralSlashCommandContext<CreateListArgs, ModalForm>.upsertHeadmateList() {
		val headmatesToIgnore = arguments.ignore?.lowercase()?.split(",")?.toSet() ?: emptySet()

		val system = try {
			getPkSystem(arguments.url)
		} catch (e: Exception) {
			respond {
				content = e.message ?: throw IllegalStateException("No error message found.")
			}

			return
		}

		val channel = this.getChannel().asChannelOfOrNull<TextChannel>() ?: run {
			respond {
				content = "Error, invalid channel, must be run in a text channel."
			}

			return
		}

		var potentialError: String? = null
		var foundHeader = false
		val foundHeadmates = mutableSetOf<String>()

		channel.withStrategy(EntitySupplyStrategy.rest)
			.getMessagesBefore(Snowflake.max)
			.filter { it.author?.id == event.kord.selfId }
			.collect {
				if (it.content == "# ${system.name} ${system.tag ?: ""}".trimEnd()) {
					foundHeader = true
				} else if (it.embeds.size == 1) {
					val embed = it.embeds.first()

					val footerText: String? = embed.footer?.text
					val headmateId = footerText?.takeLast(5)

					if (footerText == null || !footerText.startsWith("Member ID:") || headmateId == null) {
						potentialError = "Error, invalid footer found on message ${it.id}."

						return@collect
					}

					foundHeadmates.add(headmateId)
				}
			}

		if (potentialError != null) {
			respond {
				content = potentialError!!
			}

			return
		}

		val headmatesToAdd = system.members
			.filter { it.id !in foundHeadmates }
			.filter { !(it.id in headmatesToIgnore || it.name.lowercase() in headmatesToIgnore) }
			.sortedBy { it.birthday ?: it.created }

		val headerNeedsCreating = !foundHeader && headmatesToAdd.isNotEmpty()

		if (headerNeedsCreating) {
			if (foundHeadmates.isNotEmpty()) {
				respond {
					content = "Error, headmate embeds already exist without header, please delete these first."
				}

				return
			}

			channel.createMessage("# ${system.name} ${system.tag ?: ""}".trimEnd())
		}

		headmatesToAdd.forEach { headmate ->
			val headmateTitle = headmate.displayName?.let {
				val takeLength = headmate.pronouns?.length?.plus(3) ?: 0

				it.take(it.length - takeLength)
			} ?: headmate.name

			channel.createEmbed {
				title = headmateTitle
				color = headmate.color?.let { Color(it.toInt(16)) } ?: DISCORD_BLURPLE
				image = "https://raw.githubusercontent.com/CompassSystem/headmate-labeller/main/resources/filler.png"

				thumbnail {
					url = headmate.avatarUrl ?: "https://discord.com/assets/5d6a5e9d7d77ac29116e.png"
				}

				if (headmateTitle != headmate.name) {
					field {
						name = "Name"
						value = headmate.name
						inline = true
					}
				}

				headmate.pronouns?.let {
					field {
						name = "Pronouns"
						value = it
						inline = true
					}
				}

				headmate.proxyTags.let {
					if (it.isNotEmpty()) {
						field {
							name = "Proxy Tags"
							value = it.joinToString("\n") { proxy -> "`$proxy`" }
							inline = true
						}
					}
				}

				footer { text = "Member ID: ${headmate.id}" }
			}
		}

		respond {
			content = "Created " + (if (headerNeedsCreating) "header and " else "") + "${headmatesToAdd.size} headmate embeds."
		}
	}

	private suspend fun EphemeralSlashCommandContext<CreateListArgs, ModalForm>.purgeHeadmateList() {
		val headmatesToIgnore = arguments.ignore?.lowercase()?.split(",")?.toSet() ?: emptySet()

		val system = try {
			getPkSystem(arguments.url)
		} catch (e: Exception) {
			respond {
				content = e.message ?: throw IllegalStateException("No error message found.")
			}

			return
		}

		val channel = this.getChannel().asChannelOfOrNull<TextChannel>() ?: run {
			respond {
				content = "Error, invalid channel, must be run in a text channel."
			}

			return
		}

		var deletedCount = 0

		channel.withStrategy(EntitySupplyStrategy.rest)
			.getMessagesBefore(Snowflake.max)
			.filter { it.author?.id == event.kord.selfId }
			.collect {
				if (it.embeds.size == 1) {
					val embed = it.embeds.first()

					val footerText: String? = embed.footer?.text
					val headmateName = embed.fields.find { field -> field.name == "Name" }?.value ?: embed.title!!
					val headmateId = footerText?.takeLast(5)

					if (!(headmateId in headmatesToIgnore || headmateName in headmatesToIgnore)) {
						if (system.members.find {headmate -> headmate.id == headmateId } == null) {
							it.delete()

							deletedCount++
						}
					}
				}
			}

		respond {
			content = "Deleted $deletedCount headmate embeds."
		}
	}

	private suspend fun getPkSystem(url: String): PkSystem {
		val client = HttpClient(CIO) {
			install(ContentNegotiation) {
				json(Json {
					ignoreUnknownKeys = true
				})
			}
		}

		val response = try {
			client.get(url)
		} catch (e: Exception) {
			throw RuntimeException("Error: ${e.message}")
		}

		if (response.status.value != 200) {
			throw RuntimeException("Error, invalid response code: ${response.status}")
		}

		if (response.contentType()?.match(ContentType.Application.Json) == false) {
			throw RuntimeException("Error, expected json, found: ${response.contentType()}")
		}

		return try {
			response.body<PkSystem>()
		} catch (e: Exception) {
			throw RuntimeException("Error, invalid json: ${e.message}")
		}
	}
}

class CreateListArgs : Arguments() {
	val url by string {
		name = "url"
		description = "The url of the system export."
	}

	val ignore by optionalString {
		name = "ignore"
		description = "Comma separated list of headmate ids or names to ignore."
	}
}

