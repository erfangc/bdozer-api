package co.bdozer.libraries.master.calculators

import co.bdozer.core.nlp.sdk.model.AnswerQuestionRequest
import co.bdozer.core.nlp.sdk.model.CrossEncodeInput
import co.bdozer.libraries.master.models.Answer
import co.bdozer.libraries.tenk.models.TenK
import co.bdozer.libraries.utils.Beans
import org.slf4j.LoggerFactory

object QuestionAnswerMachine {

    private val coreNLP = Beans.coreNLP()
    private val log = LoggerFactory.getLogger(QuestionAnswerMachine::class.java)
    fun answerQuestion(question: String, tenKs: List<TenK>): Answer {
        log.info("Attempting to answer question '$question' across ${tenKs.size} paragraphs found in company 10-K")
        val scoredSentences = coreNLP.crossEncode(
            CrossEncodeInput().reference(question).comparisons(tenKs.map { it.text })
        ).toList()
        val context = scoredSentences.maxByOrNull { scoredSentence ->
            scoredSentence.score.toDouble()
        } ?: error("semantic search failed to find a similar paragraph to the question '$question'")

        log.info("Found best context for question '$question', score=${context.score}. Context='${context.sentence}'")
        val answerQuestion = coreNLP.answerQuestion(
            AnswerQuestionRequest().questions(listOf(question)).context(context.sentence)
        ).first()

        return Answer(
            question = question,
            answer = answerQuestion.bestAnswer,
            bestMatchContext = context.sentence,
            score = context.score.toDouble()
        )
    }
    
}