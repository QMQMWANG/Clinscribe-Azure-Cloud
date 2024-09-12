package com.example.azure;

import java.util.List;

public class AzureOpenAiRequest {
    private List<Message> messages;
    private int max_tokens;
    private double temperature;
    private double top_p;
    private int frequency_penalty;
    private int presence_penalty;
    private List<String> stop;

    public AzureOpenAiRequest(List<Message> messages, int max_tokens, double temperature, double top_p, int frequency_penalty, int presence_penalty, List<String> stop) {
        this.messages = messages;
        this.max_tokens = max_tokens;
        this.temperature = temperature;
        this.top_p = top_p;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
        this.stop = stop;
    }

    // Getters and setters

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // Getters and setters
    }
}
