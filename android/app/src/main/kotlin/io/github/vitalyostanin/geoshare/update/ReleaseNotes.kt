package io.github.vitalyostanin.geoshare.update

/** One changelog subsection of a release: its [title] and the [items] under it. */
data class NotesSection(val title: String, val items: List<String>)

/**
 * The changelog of a release as the member reads it: the [sections] in the
 * order the release lists them.
 */
data class ReleaseNotes(val sections: List<NotesSection>) {
    companion object {
        private const val RU_OPEN = "<details lang=\"ru\">"
        private const val RU_CLOSE = "</details>"
        private const val RU_SUMMARY = "<summary>"
        private const val HEADING = "### "
        private const val BULLET = "- "

        /** Subsections for the project owners; a member has no use for them. */
        private val OWNERS_TITLES = setOf("Project owners", "Владельцам проектов")
        private val LINK = Regex("""\[([^\]]*)]\([^)]*\)""")

        /**
         * Reads the `body` of a release: the English changelog section, then
         * the Russian one inside `<details lang="ru">`. Takes the block for
         * [language] ("ru" or anything else for English); a release without a
         * Russian block shows its English text to Russian too. Drops the
         * owners' subsection, joins an item's continuation lines into one,
         * and leaves the text of links and code without their markup. Null
         * if nothing is left to show.
         */
        fun parse(body: String, language: String): ReleaseNotes? {
            val text = body.replace("\r\n", "\n")
            val open = text.indexOf(RU_OPEN)
            val english = if (open >= 0) text.substring(0, open) else text
            val russian = if (open >= 0) russianBlock(text.substring(open + RU_OPEN.length)) else ""
            val chosen = if (language == "ru" && russian.isNotBlank()) russian else english
            val sections = sections(chosen.lines())

            return if (sections.isEmpty()) null else ReleaseNotes(sections)
        }

        private fun russianBlock(afterOpen: String): String {
            val inner = afterOpen.substringBefore(RU_CLOSE)

            return inner.lines()
                .filterNot { it.trim().startsWith(RU_SUMMARY) }
                .joinToString("\n")
        }

        private fun sections(lines: List<String>): List<NotesSection> {
            val result = mutableListOf<NotesSection>()
            var title: String? = null
            var items = mutableListOf<String>()

            fun close() {
                val name = title
                if (name != null && name !in OWNERS_TITLES && items.isNotEmpty()) {
                    result += NotesSection(name, items)
                }
                items = mutableListOf()
            }

            var inItem = false
            for (line in lines) {
                when {
                    line.startsWith(HEADING) -> {
                        close()
                        title = line.removePrefix(HEADING).trim()
                        inItem = false
                    }

                    line.startsWith(BULLET) -> {
                        items += clean(line.removePrefix(BULLET))
                        inItem = true
                    }

                    line.isBlank() -> inItem = false

                    inItem && line.first().isWhitespace() -> {
                        items[items.lastIndex] = items.last() + " " + clean(line)
                    }

                    else -> inItem = false
                }
            }
            close()

            return result
        }

        private fun clean(text: String): String = text.trim().replace(LINK, "$1").replace("`", "")
    }
}
