package main

import "fmt"

// matchResult is used internally to collect concurrent receives.
type matchResult struct {
	msg Msg
	ok  bool
}

// Match runs a single game between two players.
//
// Both players' messages are received concurrently. The winner is decided by
// the parity of the sum of their picks (odd → player1 wins, even → player2 wins).
// Replies are sent back to both players, and the winner's channel is then
// forwarded upstream so the next round can keep playing against them.
func Match(channel1, channel2 chan Msg, round, matchIdx int, done <-chan struct{}) chan Msg {
	outCh := make(chan Msg)
	go func() {
		r1 := make(chan matchResult, 1)
		r2 := make(chan matchResult, 1)

		go func() {
			select {
			case msg := <-channel1:
				r1 <- matchResult{msg, true}
			case <-done:
				r1 <- matchResult{ok: false}
			}
		}()
		go func() {
			select {
			case msg := <-channel2:
				r2 <- matchResult{msg, true}
			case <-done:
				r2 <- matchResult{ok: false}
			}
		}()

		res1 := <-r1
		res2 := <-r2
		if !res1.ok || !res2.ok {
			return // tournament was canceled before both players sent
		}
		left, right := res1.msg, res2.msg

		// --- determine winner ---
		sum := left.value + right.value
		leftWins := sum%2 == 1

		parity := "even"
		if leftWins {
			parity = "odd"
		}

		// notify both players of the outcome
		select {
		case left.reply <- leftWins:
		case <-done:
			return
		}
		select {
		case right.reply <- !leftWins:
		case <-done:
			return
		}

		winner, loser := right.playerId, left.playerId
		winnerCh := channel2
		if leftWins {
			winner, loser = left.playerId, right.playerId
			winnerCh = channel1
		}

		fmt.Printf("[Round %d | Match %d] %s(%d) vs %s(%d) → sum=%d (%s) → %s wins, %s out\n",
			round, matchIdx,
			left.playerId, left.value,
			right.playerId, right.value,
			sum, parity,
			winner, loser,
		)

		// --- forward the winner's subsequent picks upstream ---
		// The winner player loops and sends a fresh pick on its original channel;
		// we relay that to the next round's match (or to main for the final).
		for {
			select {
			case msg := <-winnerCh:
				select {
				case outCh <- msg:
				case <-done:
					return
				}
			case <-done:
				return
			}
		}
	}()
	return outCh
}

func spawnRound(players []chan Msg, done <-chan struct{}) chan Msg {
	return spawnRoundHelper(players, 1, done)
}

func spawnRoundHelper(players []chan Msg, round int, done <-chan struct{}) chan Msg {
	if len(players) == 2 {
		return Match(players[0], players[1], round, 0, done)
	}

	mid := len(players) / 2
	var leftMatches, rightMatches []chan Msg

	for i := 0; i < mid; i += 2 {
		leftMatches = append(leftMatches, Match(players[i], players[i+1], round, i/2, done))
	}
	for i := mid; i < len(players); i += 2 {
		rightMatches = append(rightMatches, Match(players[i], players[i+1], round, (i-mid)/2+mid/2, done))
	}

	return spawnRoundHelper(append(leftMatches, rightMatches...), round+1, done)
}
