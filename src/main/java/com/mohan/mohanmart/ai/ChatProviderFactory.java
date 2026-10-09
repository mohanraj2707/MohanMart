package com.mohan.mohanmart.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Factory selecting the configured {@link ChatProvider} implementation based on
 * environment variable {@code AI_CHATBOT_PROVIDER} or property {@code ai.chatbot.provider=gemini|mock}
 * (defaulting to {@code mock}).
 */
public final class ChatProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(ChatProviderFactory.class);

    private ChatProviderFactory() {
        // Prevent instantiation
    }

    /**
     * Creates a {@link ChatProvider} according to the environment or classpath configuration.
     *
     * @return configured {@link ChatProvider} instance
     */
    public static ChatProvider create() {
        String providerName = System.getenv("AI_CHATBOT_PROVIDER");
        String configuredModel = System.getenv("GEMINI_MODEL");

        if (providerName == null || providerName.trim().isEmpty()) {
            Properties props = loadProperties();
            providerName = props.getProperty("ai.chatbot.provider", "mock");
            if (configuredModel == null || configuredModel.trim().isEmpty()) {
                configuredModel = props.getProperty("ai.chatbot.model", GeminiChatProvider.DEFAULT_MODEL);
            }
        }

        return create(providerName, System.getenv("GEMINI_API_KEY"), configuredModel);
    }

    /**
     * Creates a {@link ChatProvider} for the given provider type, API key, and model name.
     *
     * @param providerType "gemini" or "mock" (defaults to "mock")
     * @param apiKey       Gemini API key (only used when providerType is "gemini")
     * @param model        Gemini model name (defaults to gemini-3.1-flash-lite)
     * @return {@link ChatProvider} instance
     */
    public static ChatProvider create(String providerType, String apiKey, String model) {
        String normalized = (providerType != null) ? providerType.trim().toLowerCase(Locale.ROOT) : "mock";
        if ("gemini".equals(normalized)) {
            logger.info("Initializing GeminiChatProvider with model {}", GeminiChatProvider.resolveModel(model));
            return new GeminiChatProvider(apiKey, model, null, new MockChatProvider());
        }
        logger.debug("Initializing MockChatProvider (provider={})", normalized);
        return new MockChatProvider();
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = ChatProviderFactory.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
        }
        return props;
    }
}
