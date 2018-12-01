package org.mechdancer.framework.remote.modules.group

data class Rule(
    val type: RuleType = RuleType.NONE,
    val regex: Regex = Regex("")
) {
    enum class RuleType { ACCEPT, DROP, NONE }

    infix fun decline(name: String) =
        when (type) {
            RuleType.NONE   -> false
            RuleType.ACCEPT -> !regex.matches(name)
            RuleType.DROP   -> regex.matches(name)
        }
}
