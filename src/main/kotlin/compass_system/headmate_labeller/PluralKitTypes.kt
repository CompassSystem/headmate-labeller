package compass_system.headmate_labeller

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.lang.IllegalStateException

@Serializable
data class PkSystem(
	val id: String,
	val name: String,
	val tag: String?,
	val members: List<PkMember>,
	@SerialName("avatar_url")
	val avatarUrl: String?
)

@Serializable
data class PkMember(
	val id: String,
	val name: String,
	@SerialName("display_name")
	val displayName: String?,
	val color: String?,
	val pronouns: String?,
	@SerialName("avatar_url")
	val avatarUrl: String?,
	@SerialName("proxy_tags")
	val proxyTags: List<PkProxy>
)

@Serializable
data class PkProxy(
	val prefix: String? = null,
	val suffix: String? = null
) {
	override fun toString(): String {
		if ((prefix != null && suffix != null) || (prefix == null && suffix == null)) {
			throw IllegalStateException("Proxy must have either a prefix or a suffix, but not both.")
		}

		if (prefix != null) {
			return "${prefix}text"
		}

		if (suffix != null) {
			return "text${suffix}"
		}

		throw IllegalStateException("Illegal flow control.")
	}
}
