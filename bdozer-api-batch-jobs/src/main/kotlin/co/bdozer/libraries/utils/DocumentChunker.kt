package co.bdozer.libraries.utils

import co.bdozer.core.nlp.sdk.model.DocInput
import org.slf4j.LoggerFactory

/**
 * Takes a long document (arbitrarily long) and chunk them into sentences of fixed width. If the last
 * sentence in a chunk makes the chunk goes over the pre-defined length limit, then it would be included
 * in the next chunk. The idea is to always form chunks that consists of full sentences
 */
object DocumentChunker {

    private const val chunkSize = 512
    private val coreNLP = Beans.coreNLP()
    private val log = LoggerFactory.getLogger(DocumentChunker::class.java)

    fun chunkDoc(doc: String): List<String> {
        log.info("Chunking document into sentences, doc.length=${doc.length} chunkSize=$chunkSize")
        val start = System.currentTimeMillis()
        val sentences = coreNLP
            .getSentences(DocInput().doc(doc))
            .sentences
        val stop = System.currentTimeMillis()
        var currChunk = ""
        val ret = arrayListOf<String>()
        for (sentence in sentences) {
            if (currChunk.length + sentence.length > chunkSize) {
                // start a new chunk
                ret.add(currChunk)
                currChunk = ""
            } else {
                currChunk += sentence
            }
        }
        val processingStop = System.currentTimeMillis()
        log.info(
            "Generated chunks from doc doc.length=${doc.length} " +
                    "ret.size=${ret.size} " +
                    "sentences.size=${sentences.size} " +
                    "chunking=${stop - start}ms " +
                    "processingTook=${processingStop - stop}ms"
        )
        return ret
    }

}
