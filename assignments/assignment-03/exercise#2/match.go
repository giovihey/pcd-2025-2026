package main

import "fmt"

// referee is the body of a match: an active server that serves exactly two
// requests arriving on its request channel (many-to-one communication), decides
// the winner by the parity of the sum and replies to both players.
// The player whose request arrives first plays "odds", the other plays "evens".
func referee(round, idx int, request <-chan Msg) (winner Msg) {
	odd := <-request
	even := <-request

	sum := odd.value + even.value
	oddWins := sum%2 == 1
	winner, loser := even, odd
	if oddWins {
		winner, loser = odd, even
	}

	fmt.Printf("[Round %d | Match %d] %s(%d, odds) vs %s(%d, evens) -> sum=%d -> %s wins, %s out\n",
		round, idx, odd.playerID, odd.value, even.playerID, even.value,
		sum, winner.playerID, loser.playerID)

	winner.reply <- true
	loser.reply <- false
	return winner
}

// Match is a game of any round but the last one.
func Match(round, idx int, request <-chan Msg) {
	referee(round, idx, request)
}

// Final is the last game.
func Final(round int, request <-chan Msg, champion chan<- string) {
	champion <- referee(round, 0, request).playerID
}
