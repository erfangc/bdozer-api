package co.bdozer.core.nlp.sdk.api;

import co.bdozer.core.nlp.sdk.ApiClient;
import co.bdozer.core.nlp.sdk.model.AnswerQuestionRequest;
import co.bdozer.core.nlp.sdk.model.AnswerQuestionResponse;
import co.bdozer.core.nlp.sdk.model.CrossEncodeInput;
import co.bdozer.core.nlp.sdk.model.DocInput;
import co.bdozer.core.nlp.sdk.model.HTTPValidationError;
import co.bdozer.core.nlp.sdk.model.ScoredSentence;
import co.bdozer.core.nlp.sdk.model.Sentences;
import co.bdozer.core.nlp.sdk.model.ZeroShotClassificationRequest;
import co.bdozer.core.nlp.sdk.model.ZeroShotClassificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for CoreNlpApi
 */
class CoreNlpApiTest {

    private CoreNlpApi api;

    @BeforeEach
    public void setup() {
        api = new ApiClient().buildClient(CoreNlpApi.class);
    }

    
    /**
     * Answer Question
     *
     * 
     */
    @Test
    void answerQuestionTest() {
        AnswerQuestionRequest answerQuestionRequest = null;
        // List<AnswerQuestionResponse> response = api.answerQuestion(answerQuestionRequest);

        // TODO: test validations
    }

    
    /**
     * Cross Encode
     *
     * Cross Encoder
     */
    @Test
    void crossEncodeTest() {
        CrossEncodeInput crossEncodeInput = null;
        // List<ScoredSentence> response = api.crossEncode(crossEncodeInput);

        // TODO: test validations
    }

    
    /**
     * Sentence Producer
     *
     * Turns a input document into sentences using &#x60;en_core_web_trf&#x60; transformer models
     */
    @Test
    void getSentencesTest() {
        DocInput docInput = null;
        // Sentences response = api.getSentences(docInput);

        // TODO: test validations
    }

    
    /**
     * Zero Shot Classification
     *
     * 
     */
    @Test
    void zeroShotClassificationTest() {
        ZeroShotClassificationRequest zeroShotClassificationRequest = null;
        // ZeroShotClassificationResponse response = api.zeroShotClassification(zeroShotClassificationRequest);

        // TODO: test validations
    }

    
}
