package main

import (
	"fmt"
	"math/bits"
)

// send sends v on ch, giving up if the tournament is canceled.
func send[T any](ch chan<- T, v T, done <-chan struct{}) bool {
	select {
	case ch <- v:
		return true
	case <-done:
		return false
	}
}

// play runs a single game. It collects one pick from each side (in whatever
// order they arrive), decides the winner by the parity of the sum (the left
// side plays "odds", the right side plays "evens") and replies to both players.
// It returns the input channel of the winner, so the caller can keep serving it.
func play(in1, in2 <-chan Msg, round, idx int, winResult Result, done <-chan struct{}) (winnerIn <-chan Msg, winnerID string, ok bool) {
	var left, right Msg
	c1, c2 := in1, in2
	for c1 != nil || c2 != nil { // a nil channel is never selected
		select {
		case left = <-c1:
			c1 = nil
		case right = <-c2:
			c2 = nil
		case <-done:
			return nil, "", false
		}
	}

	sum := left.value + right.value
	parity := "even"
	winner, loser, winnerIn := right, left, in2
	if sum%2 == 1 {
		parity = "odd"
		winner, loser, winnerIn = left, right, in1
	}

	// Log before replying: the winner can only reach the next round after the
	// reply, so the output order is consistent with the order of the rounds.
	fmt.Printf("[Round %d | Match %d] %s(%d) vs %s(%d) -> sum=%d (%s) -> %s wins, %s out\n",
		round, idx, left.playerID, left.value, right.playerID, right.value,
		sum, parity, winner.playerID, loser.playerID)

	if !send(winner.reply, winResult, done) || !send(loser.reply, Lost, done) {
		return nil, "", false
	}
	return winnerIn, winner.playerID, true
}

// relay forwards the picks of the winner to the next round.
func relay(from <-chan Msg, to chan<- Msg, done <-chan struct{}) {
	for {
		select {
		case msg := <-from:
			if !send(to, msg, done) {
				return
			}
		case <-done:
			return
		}
	}
}

// Match is a (non-final) game between the champions of two sub-brackets.
// The returned channel carries the picks of its winner for the next round.
// The reply channel travels inside Msg, so the next match answers the player directly.
func Match(in1, in2 <-chan Msg, round, idx int, done <-chan struct{}) <-chan Msg {
	out := make(chan Msg)
	go func() {
		winnerIn, _, ok := play(in1, in2, round, idx, Won, done)
		if ok {
			relay(winnerIn, out, done)
		}
	}()
	return out
}

// Final is the last game: the winner is announced on the returned channel.
func Final(in1, in2 <-chan Msg, round int, done <-chan struct{}) <-chan string {
	champion := make(chan string)
	go func() {
		if _, id, ok := play(in1, in2, round, 0, Champion, done); ok {
			send(champion, id, done)
		}
	}()
	return champion
}

// buildBracket builds the sub-tournament among players (whose first element has
// index `first` in the whole tournament) and returns the channel of its champion.
// A sub-bracket of n players is a game of round log2(n).
func buildBracket(players []<-chan Msg, first int, done <-chan struct{}) <-chan Msg {
	n := len(players)
	if n == 1 {
		return players[0]
	}
	mid := n / 2
	left := buildBracket(players[:mid], first, done)
	right := buildBracket(players[mid:], first+mid, done)
	return Match(left, right, bits.Len(uint(n))-1, first/n, done)
}

// Tournament builds the whole bracket (len(players) must be a power of 2, >= 2).
// The two halves are independent sub-tournaments that only meet in the final.
func Tournament(players []<-chan Msg, done <-chan struct{}) <-chan string {
	n := len(players)
	mid := n / 2
	left := buildBracket(players[:mid], 0, done)
	right := buildBracket(players[mid:], mid, done)
	return Final(left, right, bits.Len(uint(n))-1, done)
}
