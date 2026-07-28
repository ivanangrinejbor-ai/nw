package org.catrobat.catroid.dialogue

object DialogueVariableResolver {

    fun resolve(text: String, variables: Map<String, Any>): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            when {
                text[i] == '{' && i + 1 < text.length && text[i + 1] == '{' -> {
                    sb.append('{')
                    i += 2
                }
                text[i] == '}' && i + 1 < text.length && text[i + 1] == '}' -> {
                    sb.append('}')
                    i += 2
                }
                text[i] == '{' -> {
                    val end = text.indexOf('}', i + 1)
                    if (end == -1) {
                        sb.append('{')
                        i++
                    } else {
                        val varPath = text.substring(i + 1, end)
                        val value = resolveNested(varPath, variables)
                        sb.append(value ?: "{${varPath}}")
                        i = end + 1
                    }
                }
                else -> {
                    sb.append(text[i])
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun resolveNested(path: String, variables: Map<String, Any>): String? {
        val segments = parsePath(path)
        var current: Any? = variables
        for (seg in segments) {
            current = when (seg) {
                is PathSegment.Key -> {
                    when (current) {
                        is Map<*, *> -> (current as Map<String, Any>)[seg.name]
                        else -> null
                    }
                }
                is PathSegment.Index -> {
                    when (current) {
                        is List<*> -> (current as List<Any>).getOrNull(seg.index)
                        is Array<*> -> current.getOrNull(seg.index)
                        else -> null
                    }
                }
            }
            if (current == null) return null
        }
        return current?.toString()
    }

    private sealed class PathSegment {
        data class Key(val name: String) : PathSegment()
        data class Index(val index: Int) : PathSegment()
    }

    private fun parsePath(path: String): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()
        var i = 0
        while (i < path.length) {
            when {
                path[i] == '.' -> i++
                path[i] == '[' -> {
                    val close = path.indexOf(']', i + 1)
                    if (close == -1) {
                        segments.add(PathSegment.Key(path.substring(i)))
                        break
                    }
                    val idx = path.substring(i + 1, close).toIntOrNull()
                    if (idx != null) segments.add(PathSegment.Index(idx))
                    i = close + 1
                }
                else -> {
                    var end = path.indexOfAny(charArrayOf('.', '['), i)
                    if (end == -1) end = path.length
                    val key = path.substring(i, end)
                    if (key.isNotEmpty()) segments.add(PathSegment.Key(key))
                    i = end
                }
            }
        }
        return segments
    }
}
