package co.bdozer.core.nlp.sdk.api;

import co.bdozer.core.nlp.sdk.ApiClient;
import co.bdozer.core.nlp.sdk.model.DocInput;
import co.bdozer.core.nlp.sdk.model.HTTPValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for DefaultApi
 */
class DefaultApiTest {

    private DefaultApi api;

    @BeforeEach
    public void setup() {
        api = new ApiClient().buildClient(DefaultApi.class);
    }

    
    /**
     * Sentence Producer
     *
     * Turns a input document into sentences using &#x60;en_core_web_trf&#x60; transformer models
     */
    @Test
    void getSentencesTest() {
        DocInput docInput = null;
        // Object response = api.getSentences(docInput);

        // TODO: test validations
    }

    
}
