package io.github.zdyoyoyo.englishpractice.speech

/**
 * 從手機的語音引擎挑出「男聲」「女聲」（純邏輯，可測試）。
 * Android 的 Voice 沒有正式的性別欄位，只能靠名稱判斷：
 * - 名稱含 male / female（部分引擎會這樣命名）
 * - Google 語音引擎的代號：iol、iom、tpd 是男聲；iob、iog、sfg、tpc、tpf 是女聲
 */
object VoicePicker {
    enum class Gender { MALE, FEMALE, UNKNOWN }

    data class Candidate(val name: String, val locale: String, val networkRequired: Boolean)

    private val femaleWord = Regex("(^|[^a-z])female")
    private val maleWord = Regex("(^|[^a-z])male")
    private val googleMale = listOf("-iol-", "-iom-", "-tpd-")
    private val googleFemale = listOf("-iob-", "-iog-", "-sfg-", "-tpc-", "-tpf-")

    fun genderOf(voiceName: String): Gender {
        val n = voiceName.lowercase()
        return when {
            // 先看明確的 female / male 字樣（例如 "en-us-x-sfg#male_1-local"），要整個詞比對，避免 female 被當成 male
            femaleWord.containsMatchIn(n) -> Gender.FEMALE
            maleWord.containsMatchIn(n) -> Gender.MALE
            googleMale.any { it in "$n-" } -> Gender.MALE
            googleFemale.any { it in "$n-" } -> Gender.FEMALE
            else -> Gender.UNKNOWN
        }
    }

    /** 優先：美式英文 > 其他英文；離線可用 > 需要網路 */
    fun pick(voices: List<Candidate>, gender: Gender): Candidate? = voices
        .filter { it.locale.lowercase().startsWith("en") && genderOf(it.name) == gender }
        .sortedWith(compareBy({ !it.locale.lowercase().replace('_', '-').startsWith("en-us") }, { it.networkRequired }))
        .firstOrNull()
}
