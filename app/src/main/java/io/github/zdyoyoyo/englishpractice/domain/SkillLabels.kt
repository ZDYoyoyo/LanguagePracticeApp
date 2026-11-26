package io.github.zdyoyoyo.englishpractice.domain

/** 考點的中文名稱與分組（題庫 JSON 的 skills 欄位只能用這裡列出的 key，測試會檢查） */
object SkillLabels {
    enum class Group(val label: String) { GRAMMAR("文法考點"), LISTENING("聽力"), READING("閱讀"), QUESTION_TYPE("題目類型"), OTHER("其他") }

    data class Skill(val key: String, val label: String, val group: Group)

    val ALL: List<Skill> = listOf(
        Skill("preposition", "介系詞", Group.GRAMMAR),
        Skill("tense", "時態", Group.GRAMMAR),
        Skill("passive", "被動語態", Group.GRAMMAR),
        Skill("part-of-speech", "詞性判斷", Group.GRAMMAR),
        Skill("relative", "關係詞", Group.GRAMMAR),
        Skill("conjunction", "連接詞／轉折語", Group.GRAMMAR),
        Skill("comparison", "比較級／最高級", Group.GRAMMAR),
        Skill("verb-form", "不定詞／動名詞", Group.GRAMMAR),
        Skill("subjunctive", "假設語氣", Group.GRAMMAR),
        Skill("agreement", "主詞動詞一致", Group.GRAMMAR),
        Skill("pronoun", "代名詞", Group.GRAMMAR),
        Skill("inversion", "倒裝句", Group.GRAMMAR),
        Skill("quantifier", "數量詞", Group.GRAMMAR),
        Skill("sentence-insertion", "句子插入", Group.GRAMMAR),
        Skill("part2", "Part 2 應答", Group.LISTENING),
        Skill("part3", "Part 3 對話", Group.LISTENING),
        Skill("part4", "Part 4 獨白", Group.LISTENING),
        Skill("part6", "Part 6 段落填空", Group.READING),
        Skill("part7", "Part 7 閱讀理解", Group.READING),
        Skill("multi-passage", "多篇閱讀", Group.READING),
        Skill("main-idea", "主旨題", Group.QUESTION_TYPE),
        Skill("detail", "細節題", Group.QUESTION_TYPE),
        Skill("inference", "推論題", Group.QUESTION_TYPE),
        Skill("intent", "意圖題", Group.QUESTION_TYPE),
        Skill("not-question", "NOT 題", Group.QUESTION_TYPE),
        Skill("response", "應答判斷", Group.QUESTION_TYPE),
        Skill("graphic", "圖表題", Group.QUESTION_TYPE),
        Skill("vocab-zh", "單字（看英文選中文）", Group.OTHER),
        Skill("vocab-en", "單字（看中文選英文）", Group.OTHER),
        Skill("idiom", "慣用語", Group.OTHER),
        Skill("usage", "用法", Group.OTHER),
        Skill("phrasal-verb", "片語動詞", Group.OTHER),
        Skill("word-form", "詞形變化", Group.OTHER),
    )
    private val byKey = ALL.associateBy { it.key }

    fun label(key: String) = byKey[key]?.label ?: key
    fun isKnown(key: String) = key in byKey
    fun inGroup(group: Group) = ALL.filter { it.group == group }
}
