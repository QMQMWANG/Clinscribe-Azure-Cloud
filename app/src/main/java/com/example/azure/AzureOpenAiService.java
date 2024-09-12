package com.example.azure;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AzureOpenAiService {
    @Headers({
            "Content-Type: application/json",
            "api-key: b951fee60d8d458d854e34870c4d7f5a" // replace with your actual API key
    })
    @POST("openai/deployments/gpt-4o/chat/completions?api-version=2024-02-15-preview")
    Call<AzureOpenAiResponse> getDetailedDescription(@Body AzureOpenAiRequest request);
}
