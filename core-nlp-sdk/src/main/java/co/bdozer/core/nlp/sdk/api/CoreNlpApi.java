package co.bdozer.core.nlp.sdk.api;

import co.bdozer.core.nlp.sdk.ApiClient;
import co.bdozer.core.nlp.sdk.EncodingUtils;
import co.bdozer.core.nlp.sdk.model.ApiResponse;

import co.bdozer.core.nlp.sdk.model.AnswerQuestionRequest;
import co.bdozer.core.nlp.sdk.model.AnswerQuestionResponse;
import co.bdozer.core.nlp.sdk.model.CrossEncodeInput;
import co.bdozer.core.nlp.sdk.model.DocInput;
import co.bdozer.core.nlp.sdk.model.HTTPValidationError;
import co.bdozer.core.nlp.sdk.model.ScoredSentence;
import co.bdozer.core.nlp.sdk.model.Sentences;
import co.bdozer.core.nlp.sdk.model.ZeroShotClassificationRequest;
import co.bdozer.core.nlp.sdk.model.ZeroShotClassificationResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-04-22T19:20:31.952-04:00[America/New_York]")
public interface CoreNlpApi extends ApiClient.Api {


  /**
   * Answer Question
   * 
   * @param answerQuestionRequest  (required)
   * @return List&lt;AnswerQuestionResponse&gt;
   */
  @RequestLine("POST /answer_question")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<AnswerQuestionResponse> answerQuestion(AnswerQuestionRequest answerQuestionRequest);

  /**
   * Answer Question
   * Similar to <code>answerQuestion</code> but it also returns the http response headers .
   * 
   * @param answerQuestionRequest  (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /answer_question")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<AnswerQuestionResponse>> answerQuestionWithHttpInfo(AnswerQuestionRequest answerQuestionRequest);



  /**
   * Cross Encode
   * Cross Encoder
   * @param crossEncodeInput  (required)
   * @return List&lt;ScoredSentence&gt;
   */
  @RequestLine("POST /cross_encode")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<ScoredSentence> crossEncode(CrossEncodeInput crossEncodeInput);

  /**
   * Cross Encode
   * Similar to <code>crossEncode</code> but it also returns the http response headers .
   * Cross Encoder
   * @param crossEncodeInput  (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /cross_encode")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<ScoredSentence>> crossEncodeWithHttpInfo(CrossEncodeInput crossEncodeInput);



  /**
   * Sentence Producer
   * Turns a input document into sentences using &#x60;en_core_web_trf&#x60; transformer models
   * @param docInput  (required)
   * @return Sentences
   */
  @RequestLine("POST /sentence_producer")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  Sentences getSentences(DocInput docInput);

  /**
   * Sentence Producer
   * Similar to <code>getSentences</code> but it also returns the http response headers .
   * Turns a input document into sentences using &#x60;en_core_web_trf&#x60; transformer models
   * @param docInput  (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /sentence_producer")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<Sentences> getSentencesWithHttpInfo(DocInput docInput);



  /**
   * Zero Shot Classification
   * 
   * @param zeroShotClassificationRequest  (required)
   * @return ZeroShotClassificationResponse
   */
  @RequestLine("POST /zero_shot_classification")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ZeroShotClassificationResponse zeroShotClassification(ZeroShotClassificationRequest zeroShotClassificationRequest);

  /**
   * Zero Shot Classification
   * Similar to <code>zeroShotClassification</code> but it also returns the http response headers .
   * 
   * @param zeroShotClassificationRequest  (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /zero_shot_classification")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<ZeroShotClassificationResponse> zeroShotClassificationWithHttpInfo(ZeroShotClassificationRequest zeroShotClassificationRequest);


}
