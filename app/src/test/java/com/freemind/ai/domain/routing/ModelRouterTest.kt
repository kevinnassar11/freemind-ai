package com.freemind.ai.domain.routing

import com.freemind.ai.domain.model.AiModel
import com.freemind.ai.domain.model.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the classifier and the routing table in isolation - no Android
 * framework, no coroutines, no network. Pure JVM unit tests.
 */
class ModelRouterTest {

    private fun classify(message: String): QueryClass =
        HeuristicQueryClassifier.classify(message)

    // --- Classification ----------------------------------------------------

    @Test
    fun `short factual question is simple`() {
        assertEquals(QueryClass.SIMPLE_FACTUAL, classify("capital of France?"))
        assertEquals(QueryClass.SIMPLE_FACTUAL, classify("who wrote Dune"))
    }

    @Test
    fun `why questions are reasoning heavy`() {
        assertEquals(QueryClass.REASONING_HEAVY, classify("Why does inflation reduce purchasing power?"))
    }

    @Test
    fun `multi part question is reasoning heavy`() {
        assertEquals(
            QueryClass.REASONING_HEAVY,
            classify("What is a bond? How does it differ from a stock?"),
        )
    }

    @Test
    fun `code fence is coding`() {
        assertEquals(QueryClass.CODING, classify("fix this\n```\nval x = \n```"))
    }

    @Test
    fun `debugging language is coding`() {
        assertEquals(QueryClass.CODING, classify("I keep getting a null pointer exception on line 12"))
    }

    @Test
    fun `writing request is long form`() {
        assertEquals(
            QueryClass.LONG_FORM_WRITING,
            classify("Please draft an email to my landlord about the broken heating"),
        )
    }

    @Test
    fun `think carefully phrasing is hard technical`() {
        assertEquals(
            QueryClass.HARD_TECHNICAL,
            classify("Think carefully: is this distributed lock implementation correct?"),
        )
    }

    @Test
    fun `hard technical beats coding when both present`() {
        // Explicit "think carefully" should win over the code keywords.
        assertEquals(
            QueryClass.HARD_TECHNICAL,
            classify("Think carefully about this Kotlin function and prove it terminates"),
        )
    }

    @Test
    fun `word boundary prevents false coding match`() {
        // "decode" must not trigger on the "code" keyword.
        assertEquals(QueryClass.SIMPLE_FACTUAL, classify("decode this"))
    }

    @Test
    fun `blank message falls back to everyday`() {
        assertEquals(QueryClass.EVERYDAY, classify("   "))
    }

    // --- Mapping -----------------------------------------------------------

    @Test
    fun `each class maps to its model`() {
        assertEquals(AiModel.SWIFT, ModelRouter.modelFor(QueryClass.SIMPLE_FACTUAL))
        assertEquals(AiModel.CORE, ModelRouter.modelFor(QueryClass.EVERYDAY))
        assertEquals(AiModel.SAGE, ModelRouter.modelFor(QueryClass.REASONING_HEAVY))
        assertEquals(AiModel.SPARK, ModelRouter.modelFor(QueryClass.CODING))
        assertEquals(AiModel.VERSE, ModelRouter.modelFor(QueryClass.LONG_FORM_WRITING))
        assertEquals(AiModel.ORACLE, ModelRouter.modelFor(QueryClass.HARD_TECHNICAL))
    }

    // --- Tier clamping -----------------------------------------------------

    @Test
    fun `pro tier keeps the pro models`() {
        assertEquals(AiModel.ORACLE, ModelRouter.clampToTier(AiModel.ORACLE, SubscriptionTier.PRO))
        assertEquals(AiModel.SAGE, ModelRouter.clampToTier(AiModel.SAGE, SubscriptionTier.PRO))
    }

    @Test
    fun `free tier substitutes locked models`() {
        assertEquals(AiModel.VERSE, ModelRouter.clampToTier(AiModel.ORACLE, SubscriptionTier.FREE))
        assertEquals(AiModel.CORE, ModelRouter.clampToTier(AiModel.SAGE, SubscriptionTier.FREE))
    }

    @Test
    fun `free tier leaves unlocked models alone`() {
        assertEquals(AiModel.SWIFT, ModelRouter.clampToTier(AiModel.SWIFT, SubscriptionTier.FREE))
        assertEquals(AiModel.SPARK, ModelRouter.clampToTier(AiModel.SPARK, SubscriptionTier.FREE))
    }

    // --- End-to-end routing ------------------------------------------------

    @Test
    fun `free user asking a hard question never gets a locked model`() {
        val routed = ModelRouter.route(
            "Think carefully and prove this algorithm is correct",
            SubscriptionTier.FREE,
        )
        assertEquals(AiModel.VERSE, routed)
    }

    @Test
    fun `classifier failure falls back to default`() {
        val exploding = QueryClassifier { error("classifier boom") }
        assertEquals(
            AiModel.DEFAULT,
            ModelRouter.route("anything", SubscriptionTier.PRO, exploding),
        )
    }

    @Test
    fun `custom classifier can be swapped in`() {
        // Proves option (b) - a cheap-model classifier - drops in without a rewrite.
        val alwaysCoding = QueryClassifier { QueryClass.CODING }
        assertEquals(
            AiModel.SPARK,
            ModelRouter.route("what time is it", SubscriptionTier.FREE, alwaysCoding),
        )
    }
}
