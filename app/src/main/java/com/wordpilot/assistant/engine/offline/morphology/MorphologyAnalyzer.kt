package com.wordpilot.assistant.engine.offline.morphology

import com.wordpilot.assistant.engine.offline.model.POSTag

data class VerbInflections(
    val base: String,
    val thirdPersonSingular: String,
    val past: String,
    val pastParticiple: String,
    val presentParticiple: String
)

enum class VerbForm {
    BASE,
    THIRD_PERSON_SINGULAR,
    PAST,
    PAST_PARTICIPLE,
    PRESENT_PARTICIPLE
}

data class VerbAnalysis(
    val lemma: String,
    val form: VerbForm
)

class MorphologyAnalyzer {

    // Common irregular verbs table
    private val irregularVerbs = mapOf(
        "be" to VerbInflections("be", "is", "was", "been", "being"),
        "have" to VerbInflections("have", "has", "had", "had", "having"),
        "do" to VerbInflections("do", "does", "did", "done", "doing"),
        "go" to VerbInflections("go", "goes", "went", "gone", "going"),
        "see" to VerbInflections("see", "sees", "saw", "seen", "seeing"),
        "write" to VerbInflections("write", "writes", "wrote", "written", "writing"),
        "take" to VerbInflections("take", "takes", "took", "taken", "taking"),
        "get" to VerbInflections("get", "gets", "got", "gotten", "getting"),
        "give" to VerbInflections("give", "gives", "gave", "given", "giving"),
        "make" to VerbInflections("make", "makes", "made", "made", "making"),
        "know" to VerbInflections("know", "knows", "knew", "known", "knowing"),
        "think" to VerbInflections("think", "thinks", "thought", "thought", "thinking"),
        "come" to VerbInflections("come", "comes", "came", "come", "coming"),
        "say" to VerbInflections("say", "says", "said", "said", "saying"),
        "find" to VerbInflections("find", "finds", "found", "found", "finding"),
        "tell" to VerbInflections("tell", "tells", "told", "told", "telling"),
        "become" to VerbInflections("become", "becomes", "became", "become", "becoming"),
        "show" to VerbInflections("show", "shows", "showed", "shown", "showing"),
        "leave" to VerbInflections("leave", "leaves", "left", "left", "leaving"),
        "feel" to VerbInflections("feel", "feels", "felt", "felt", "feeling"),
        "bring" to VerbInflections("bring", "brings", "brought", "brought", "bringing"),
        "begin" to VerbInflections("begin", "begins", "began", "begun", "beginning"),
        "keep" to VerbInflections("keep", "keeps", "kept", "kept", "keeping"),
        "hold" to VerbInflections("hold", "holds", "held", "held", "holding"),
        "stand" to VerbInflections("stand", "stands", "stood", "stood", "standing"),
        "hear" to VerbInflections("hear", "hears", "heard", "heard", "hearing"),
        "let" to VerbInflections("let", "lets", "let", "let", "letting"),
        "mean" to VerbInflections("mean", "means", "meant", "meant", "meaning"),
        "set" to VerbInflections("set", "sets", "set", "set", "setting"),
        "meet" to VerbInflections("meet", "meets", "met", "met", "meeting"),
        "run" to VerbInflections("run", "runs", "ran", "run", "running"),
        "pay" to VerbInflections("pay", "pays", "paid", "paid", "paying"),
        "sit" to VerbInflections("sit", "sits", "sat", "sat", "sitting"),
        "speak" to VerbInflections("speak", "speaks", "spoke", "spoken", "speaking"),
        "lie" to VerbInflections("lie", "lies", "lay", "lain", "lying"),
        "lead" to VerbInflections("lead", "leads", "led", "led", "leading"),
        "read" to VerbInflections("read", "reads", "read", "read", "reading"),
        "grow" to VerbInflections("grow", "grows", "grew", "grown", "growing"),
        "lose" to VerbInflections("lose", "loses", "lost", "lost", "losing"),
        "fall" to VerbInflections("fall", "falls", "fell", "fallen", "falling"),
        "send" to VerbInflections("send", "sends", "sent", "sent", "sending"),
        "build" to VerbInflections("build", "builds", "built", "built", "building"),
        "understand" to VerbInflections("understand", "understands", "understood", "understood", "understanding"),
        "draw" to VerbInflections("draw", "draws", "drew", "drawn", "drawing"),
        "break" to VerbInflections("break", "breaks", "broke", "broken", "breaking"),
        "spend" to VerbInflections("spend", "spends", "spent", "spent", "spending"),
        "cut" to VerbInflections("cut", "cuts", "cut", "cut", "cutting"),
        "rise" to VerbInflections("rise", "rises", "rose", "risen", "rising"),
        "drive" to VerbInflections("drive", "drives", "drove", "driven", "driving"),
        "buy" to VerbInflections("buy", "buys", "bought", "bought", "buying"),
        "wear" to VerbInflections("wear", "wears", "wore", "worn", "wearing"),
        "choose" to VerbInflections("choose", "chooses", "chose", "chosen", "choosing"),
        "fly" to VerbInflections("fly", "flies", "flew", "flown", "flying"),
        "forget" to VerbInflections("forget", "forgets", "forgot", "forgotten", "forgetting"),
        "drink" to VerbInflections("drink", "drinks", "drank", "drunk", "drinking"),
        "sing" to VerbInflections("sing", "sings", "sang", "sung", "singing"),
        "swim" to VerbInflections("swim", "swims", "swam", "swum", "swimming"),
        "eat" to VerbInflections("eat", "eats", "ate", "eaten", "eating"),
        "throw" to VerbInflections("throw", "throws", "threw", "thrown", "throwing"),
        "freeze" to VerbInflections("freeze", "freezes", "froze", "frozen", "freezing"),
        "hide" to VerbInflections("hide", "hides", "hid", "hidden", "hiding"),
        "bite" to VerbInflections("bite", "bites", "bit", "bitten", "biting"),
        "ride" to VerbInflections("ride", "rides", "rode", "ridden", "riding"),
        "steal" to VerbInflections("steal", "steals", "stole", "stolen", "stealing"),
        "wake" to VerbInflections("wake", "wakes", "woke", "woken", "waking"),
        "blow" to VerbInflections("blow", "blows", "blew", "blown", "blowing"),
        "catch" to VerbInflections("catch", "catches", "caught", "caught", "catching"),
        "teach" to VerbInflections("teach", "teaches", "taught", "taught", "teaching"),
        "fight" to VerbInflections("fight", "fights", "fought", "fought", "fighting"),
        "sleep" to VerbInflections("sleep", "sleeps", "slept", "slept", "sleeping"),
        "strike" to VerbInflections("strike", "strikes", "struck", "struck", "striking"),
        "win" to VerbInflections("win", "wins", "won", "won", "winning"),
        "sell" to VerbInflections("sell", "sells", "sold", "sold", "selling"),
        "shine" to VerbInflections("shine", "shines", "shone", "shone", "shining"),
        "shoot" to VerbInflections("shoot", "shoots", "shot", "shot", "shooting"),
        "lend" to VerbInflections("lend", "lends", "lent", "lent", "lending")
    )

    // Reverse lookup map: word -> list of VerbAnalysis
    private val reverseVerbMap = mutableMapOf<String, MutableList<VerbAnalysis>>()

    init {
        for ((lemma, infl) in irregularVerbs) {
            addReverse(lemma, lemma, VerbForm.BASE)
            addReverse(infl.thirdPersonSingular, lemma, VerbForm.THIRD_PERSON_SINGULAR)
            addReverse(infl.past, lemma, VerbForm.PAST)
            addReverse(infl.pastParticiple, lemma, VerbForm.PAST_PARTICIPLE)
            addReverse(infl.presentParticiple, lemma, VerbForm.PRESENT_PARTICIPLE)
        }
        // Special forms for "be"
        addReverse("am", "be", VerbForm.BASE)
        addReverse("are", "be", VerbForm.BASE)
        addReverse("were", "be", VerbForm.PAST)
    }

    private fun addReverse(word: String, lemma: String, form: VerbForm) {
        reverseVerbMap.computeIfAbsent(word.lowercase()) { mutableListOf() }.add(VerbAnalysis(lemma, form))
    }

    // Irregular noun plurals
    private val singularToPluralNouns = mapOf(
        "child" to "children",
        "person" to "people",
        "man" to "men",
        "woman" to "women",
        "foot" to "feet",
        "tooth" to "teeth",
        "mouse" to "mice",
        "goose" to "geese",
        "ox" to "oxen",
        "criterion" to "criteria",
        "phenomenon" to "phenomena",
        "datum" to "data",
        "medium" to "media",
        "analysis" to "analyses",
        "crisis" to "crises",
        "thesis" to "theses",
        "hypothesis" to "hypotheses",
        "basis" to "bases",
        "leaf" to "leaves",
        "life" to "lives",
        "knife" to "knives",
        "wife" to "wives",
        "half" to "halves",
        "shelf" to "shelves",
        "thief" to "thieves",
        "wolf" to "wolves",
        "sheep" to "sheep",
        "deer" to "deer",
        "fish" to "fish",
        "species" to "species",
        "aircraft" to "aircraft"
    )

    private val pluralToSingularNouns = mutableMapOf<String, String>().apply {
        for ((s, p) in singularToPluralNouns) {
            put(p, s)
        }
    }

    // Uncountable / Mass nouns
    val uncountableNouns = hashSetOf(
        "information", "advice", "furniture", "luggage", "baggage", "equipment", "traffic",
        "accommodation", "software", "hardware", "evidence", "research", "knowledge", "news",
        "homework", "money", "water", "milk", "air", "rice", "sugar", "bread", "butter",
        "music", "art", "weather", "luck", "progress", "chaos", "garbage", "trash", "poetry",
        "scenery", "machinery", "work", "behavior", "clothing", "jewelry"
    )

    fun getIrregularVerb(lemma: String): VerbInflections? = irregularVerbs[lemma.lowercase()]

    fun analyzeVerbForm(word: String): List<VerbAnalysis> {
        val lower = word.lowercase()
        return reverseVerbMap[lower] ?: emptyList()
    }

    fun isIrregularPastParticiple(word: String): Boolean {
        val forms = analyzeVerbForm(word)
        return forms.any { it.form == VerbForm.PAST_PARTICIPLE }
    }

    fun isIrregularPast(word: String): Boolean {
        val forms = analyzeVerbForm(word)
        return forms.any { it.form == VerbForm.PAST }
    }

    fun getPastParticiple(lemma: String): String {
        val irregular = irregularVerbs[lemma.lowercase()]
        if (irregular != null) return irregular.pastParticiple

        // Regular past participle is same as regular past (-ed)
        return getRegularPast(lemma)
    }

    fun getPastTense(lemma: String): String {
        val irregular = irregularVerbs[lemma.lowercase()]
        if (irregular != null) return irregular.past
        return getRegularPast(lemma)
    }

    fun getThirdPersonSingularVerb(lemma: String): String {
        val irregular = irregularVerbs[lemma.lowercase()]
        if (irregular != null) return irregular.thirdPersonSingular

        val lower = lemma.lowercase()
        return when {
            lower.endsWith("ss") || lower.endsWith("sh") || lower.endsWith("ch") || lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("o") -> lower + "es"
            lower.endsWith("y") && lower.length > 1 && !isVowel(lower[lower.length - 2]) -> lower.dropLast(1) + "ies"
            else -> lower + "s"
        }
    }

    fun getBaseVerb(word: String): String {
        val analyses = analyzeVerbForm(word)
        if (analyses.isNotEmpty()) return analyses.first().lemma

        val lower = word.lowercase()
        return when {
            lower.endsWith("ies") -> lower.dropLast(3) + "y"
            lower.endsWith("es") && (lower.dropLast(2).endsWith("sh") || lower.dropLast(2).endsWith("ch") || lower.dropLast(2).endsWith("ss") || lower.dropLast(2).endsWith("x")) -> lower.dropLast(2)
            lower.endsWith("ed") -> {
                val stem = lower.dropLast(2)
                if (stem.endsWith("i")) stem.dropLast(1) + "y"
                else if (stem.length > 2 && stem.last() == stem[stem.length - 2]) stem.dropLast(1)
                else stem
            }
            lower.endsWith("ing") -> {
                val stem = lower.dropLast(3)
                if (stem.length > 2 && stem.last() == stem[stem.length - 2]) stem.dropLast(1)
                else stem
            }
            lower.endsWith("s") && !lower.endsWith("ss") -> lower.dropLast(1)
            else -> lower
        }
    }

    fun getPluralNoun(singular: String): String {
        val irregular = singularToPluralNouns[singular.lowercase()]
        if (irregular != null) return irregular

        val lower = singular.lowercase()
        return when {
            lower.endsWith("s") || lower.endsWith("sh") || lower.endsWith("ch") || lower.endsWith("x") || lower.endsWith("z") -> lower + "es"
            lower.endsWith("y") && lower.length > 1 && !isVowel(lower[lower.length - 2]) -> lower.dropLast(1) + "ies"
            lower.endsWith("f") -> lower.dropLast(1) + "ves"
            lower.endsWith("fe") -> lower.dropLast(2) + "ves"
            else -> lower + "s"
        }
    }

    fun getSingularNoun(plural: String): String {
        val irregular = pluralToSingularNouns[plural.lowercase()]
        if (irregular != null) return irregular

        val lower = plural.lowercase()
        return when {
            lower.endsWith("ies") -> lower.dropLast(3) + "y"
            lower.endsWith("ves") -> lower.dropLast(3) + "f"
            lower.endsWith("es") && (lower.dropLast(2).endsWith("sh") || lower.dropLast(2).endsWith("ch") || lower.dropLast(2).endsWith("ss") || lower.dropLast(2).endsWith("x") || lower.dropLast(2).endsWith("z")) -> lower.dropLast(2)
            lower.endsWith("s") && !lower.endsWith("ss") -> lower.dropLast(1)
            else -> lower
        }
    }

    fun isPluralNoun(word: String): Boolean {
        val lower = word.lowercase()
        if (lower in pluralToSingularNouns) return true
        if (lower in uncountableNouns) return false
        return lower.endsWith("s") && !lower.endsWith("ss") && lower.length > 2
    }

    fun isUncountable(word: String): Boolean = word.lowercase() in uncountableNouns

    private fun getRegularPast(lemma: String): String {
        val lower = lemma.lowercase()
        return when {
            lower.endsWith("e") -> lower + "d"
            lower.endsWith("y") && lower.length > 1 && !isVowel(lower[lower.length - 2]) -> lower.dropLast(1) + "ied"
            needsConsonantDoubling(lower) -> lower + lower.last() + "ed"
            else -> lower + "ed"
        }
    }

    private fun needsConsonantDoubling(word: String): Boolean {
        if (word.length in 3..4 && !isVowel(word.last()) && isVowel(word[word.length - 2]) && !isVowel(word[word.length - 3])) {
            val lastChar = word.last()
            return lastChar !in setOf('w', 'x', 'y')
        }
        return false
    }

    private fun isVowel(c: Char): Boolean = c in setOf('a', 'e', 'i', 'o', 'u')
}
