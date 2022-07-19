package remover

import arc.struct.Seq
import arc.util.Strings
import mindustry.Vars.player
import mindustry.gen.Building
import mindustry.world.blocks.logic.LogicBlock.LogicBuild

object ProcessorPatcher {
    private val attemMatcher =
        "(ubind @?[^ ]+\\n)sensor (\\S+) @unit @flag\\nop add (\\S+) \\3 1\\njump \\d+ greaterThanEq \\3 \\d+\\njump \\d+ notEqual ([^ ]+) \\2\\nset \\3 0".toRegex()

    private val jumpMatcher = "jump (\\d+)(.*)".toRegex()

    fun countProcessors(builds: Seq<Building>) =
        builds.count { it.team == player.team() && it is LogicBuild && attemMatcher.containsMatchIn(it.code) }

    fun patch(code: String): String {
        val result = attemMatcher.find(code) ?: return code

        val groups = result.groupValues
        val bindLine = (0..result.range.first).count { code[it] == '\n' }
        return buildString {
            replaceJumps(this, code.substring(0, result.range.first), bindLine)
            append(groups[1])
            append("sensor ").append(groups[2]).append(" @unit @flag\n")
            append("jump ").append(bindLine).append(" notEqual ").append(groups[2]).append(' ').append(groups[4])
                .append('\n')
            replaceJumps(this, code.substring(result.range.last + 1), bindLine)
        }
    }

    private fun replaceJumps(sb: StringBuilder, code: String, bindLine: Int) {
        val matches = jumpMatcher.findAll(code).toList()
        val extra = sb.length
        sb.append(code)
        matches.forEach {
            val group = it.groups[1]!!
            val line = Strings.parseInt(group.value)
            if (line >= bindLine) sb.setRange(
                group.range.first + extra,
                group.range.last + extra + 1,
                (line - 3).toString()
            )
        }
    }
}