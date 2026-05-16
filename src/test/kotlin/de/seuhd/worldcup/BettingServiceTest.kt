package de.seuhd.worldcup

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BettingServiceTest {

    private fun match(id: Int, home: String, away: String, hs: Int?, aws: Int?) =
        Match(
            matchId = id,
            round = "Matchday 1",
            date = "2026-06-01",
            homeTeam = home,
            awayTeam = away,
            homeScore = hs,
            awayScore = aws,
            ground = "Test Stadium"
        )

    @BeforeTest
    fun resetBets() {
        BettingService.clear()
    }

    // ── evaluateBonus ──────────────────────────────────────────────────────────

    @Test
    fun `evaluateBonus awards 3 points for an exact score prediction`() {
        val bet = Bet(1, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        val testMatch = match(1, "Home", "Away", 5, 3)
        val testMatches = listOf(testMatch)
        val totalBonus = BettingService.evaluateBonus(testMatches)

        assertEquals(3, totalBonus)
    }

    @Test
    fun `evaluateBonus awards 1 point for correct outcome without exact score`() {
        val bet = Bet(1, Prediction.HOME_WIN, 4, 3)
        BettingService.placeBet(bet)

        val testMatch = match(1, "Home", "Away", 5, 3)
        val testMatches = listOf(testMatch)
        val totalBonus = BettingService.evaluateBonus(testMatches)

        assertEquals(1, totalBonus)
    }

    @Test
    fun `evaluateBonus awards 0 points for a wrong prediction`() {
        val bet = Bet(1, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        val testMatch = match(1, "Home", "Away", 3, 5)
        val testMatches = listOf(testMatch)
        val totalBonus = BettingService.evaluateBonus(testMatches)

        assertEquals(0, totalBonus)
    }

    @Test
    fun `evaluateBonus ignores unplayed matches`() {
        val bet = Bet(1, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        val testMatch = match(1, "Home", "Away", null, null)
        val testMatches = listOf(testMatch)
        val totalBonus = BettingService.evaluateBonus(testMatches)

        assertEquals(0, totalBonus)
    }

    // ── removeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `removeBet removes an existing bet so it no longer affects evaluation`() {
        val matchId = 1
        val bet = Bet(matchId, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        BettingService.removeBet(matchId)

        val testMatch = match(matchId, "Home", "Away", 5, 3)
        val testMatches = listOf(testMatch)
        val bettingResult = BettingService.evaluate(testMatches)

        assertEquals(0, bettingResult.evaluated)
    }

    @Test
    fun `removeBet does nothing when no bet exists for that matchId`() {
        val matchId = 1
        val anotherMatchId = 2
        val bet = Bet(matchId, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        BettingService.removeBet(anotherMatchId)

        val testMatch = match(matchId, "Home", "Away", 5, 3)
        val testMatches = listOf(testMatch)
        val bettingResult = BettingService.evaluate(testMatches)

        assertEquals(1, bettingResult.evaluated)
    }

    // ── changeBet ─────────────────────────────────────────────────────────────

    @Test
    fun `changeBet updates the prediction for an existing bet`() {
        val matchId = 1
        val bet = Bet(matchId, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        val newBet = Bet(matchId, Prediction.AWAY_WIN, 3, 5)
        BettingService.changeBet(newBet)

        val testMatch = match(matchId, "Home", "Away", 5, 3)
        val testMatches = listOf(testMatch)

        var bettingResult = BettingService.evaluate(testMatches)
        bettingResult = BettingService.evaluate(testMatches)
        val totalBonus = BettingService.evaluateBonus(testMatches)

        println(bettingResult)
        assertEquals(1, bettingResult.evaluated)
        assertEquals(1, bettingResult.incorrect)
        assertEquals(0, totalBonus)
    }

    @Test
    fun `changeBet throws when no bet exists for that matchId`() {
        val matchId = 1
        val anotherMatchId = 2
        val bet = Bet(matchId, Prediction.HOME_WIN, 5, 3)
        BettingService.placeBet(bet)

        val newBet = Bet(anotherMatchId, Prediction.AWAY_WIN, 3, 5)

        assertFailsWith<IllegalArgumentException>(
            message = "No bet has been found for that match.",
            block = {
                BettingService.changeBet(newBet)
            }
        )
    }
}