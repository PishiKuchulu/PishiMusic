package com.tuned.app.data.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TtmlDuetTest {

    private fun ttml(body: String, agents: String = "") = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
          <head><metadata>$agents</metadata></head>
          <body><div>$body</div></body>
        </tt>
    """.trimIndent()

    @Test
    fun secondSingerGetsTheOtherSide() {
        val lines = TtmlParser.parse(
            ttml(
                """
                <p begin="1.0" end="2.0" ttm:agent="v1">First voice</p>
                <p begin="2.0" end="3.0" ttm:agent="v2">Second voice</p>
                <p begin="3.0" end="4.0" ttm:agent="v1">First again</p>
                """,
                agents = """<ttm:agent type="person" xml:id="v1"/><ttm:agent type="person" xml:id="v2"/>""",
            ),
        )
        assertThat(lines.map { it.vocalist }).containsExactly(0, 1, 0).inOrder()
    }

    @Test
    fun aChorusCountsAsTheLead() {
        val lines = TtmlParser.parse(
            ttml(
                """
                <p begin="1.0" end="2.0" ttm:agent="v1">Lead</p>
                <p begin="2.0" end="3.0" ttm:agent="v1000">Everyone</p>
                """,
                agents = """<ttm:agent type="person" xml:id="v1"/><ttm:agent type="group" xml:id="v1000"/>""",
            ),
        )
        assertThat(lines.map { it.vocalist }).containsExactly(0, 0).inOrder()
    }

    @Test
    fun aSongWithoutAgentsIsNotADuet() {
        val lines = TtmlParser.parse(ttml("""<p begin="1.0" end="2.0">Solo</p><p begin="2.0" end="3.0">Still solo</p>"""))
        assertThat(lines.map { it.vocalist }).containsExactly(0, 0)
    }

    @Test
    fun theAgentCanSitOnTheSection() {
        val doc = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div ttm:agent="v1"><p begin="1.0" end="2.0">One</p></div>
                <div ttm:agent="v2"><p begin="2.0" end="3.0">Two</p></div>
              </body>
            </tt>
        """.trimIndent()
        assertThat(TtmlParser.parse(doc).map { it.vocalist }).containsExactly(0, 1).inOrder()
    }
}
