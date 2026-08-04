package org.catrobat.catroid.ai.model

enum class AiProvider(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultModels: List<String>
) {
    GEMINI(
        "gemini",
        "Google Gemini",
        "https://generativelanguage.googleapis.com/v1beta/",
        listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )
    ),
    OPENAI(
        "openai",
        "OpenAI",
        "https://api.openai.com/v1/",
        listOf(
            "gpt-4o",
            "gpt-4o-mini",
            "o1-preview",
            "o3-mini",
            "gpt-4-turbo",
            "gpt-3.5-turbo"
        )
    ),
    DEEPSEEK(
        "deepseek",
        "DeepSeek",
        "https://api.deepseek.com/v1/",
        listOf(
            "deepseek-chat",
            "deepseek-reasoner"
        )
    ),
    OPENROUTER(
        "openrouter",
        "OpenRouter",
        "https://openrouter.ai/api/v1/",
        listOf(
            "anthropic/claude-3.5-sonnet",
            "deepseek/deepseek-r1",
            "openai/gpt-4o",
            "google/gemini-2.5-flash",
            "meta-llama/llama-3.3-70b-instruct"
        )
    ),
    CLAUDE(
        "claude",
        "Anthropic Claude",
        "https://api.anthropic.com/v1/",
        listOf(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229"
        )
    ),
    OPENCODE(
        "opencode",
        "OpenCode",
        "https://opencode.ai/zen/v1/",
        listOf(
            "deepseek-v4-flash-free",
            "mimo-v2.5-free",
            "laguna-s-2.1-free",
            "ling-3.0-flash-free",
            "north-mini-code-free",
            "nemotron-3-ultra-free",
            "big-pickle"
        )
    );

    companion object {
        fun fromId(id: String?): AiProvider {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GEMINI
        }
    }
}
