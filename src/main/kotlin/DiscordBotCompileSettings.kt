object DiscordBotCompileSettings {
    val running: RunType = RunType.DiscordBot

    const val CHAT_GPT_URL: String = "http://127.0.0.1:8000/"
    const val CHAT_GPT_KEY: String = "sk-"
    const val STABLE_DIFFUSION_URL: String = "http://127.0.0.1:7860/sdapi/v1"
}

enum class RunType {
    DiscordBot,
    Testing
}

