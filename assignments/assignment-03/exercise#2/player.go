package main

import "math/rand/v2"

// Result is what a match tells a player about the game it has just played.
type Result int

const (
	Lost     Result = iota // eliminated: stop playing
	Won                    // won this round: send a new pick for the next one
	Champion               // won the final: the tournament is over
)

// Msg is the only data exchanged between players and matches.
// The reply channel is where the match sends the outcome back to the player
// (the same request/reply pattern used for client-server interaction).
type Msg struct {
	value    int
	playerID string
	reply    chan Result
}

// Player repeatedly sends a pick on out and waits for the outcome of the game,
// until it is eliminated, becomes champion, or the tournament is cancelled.
func Player(out chan<- Msg, id string, done <-chan struct{}) {
	for {
		reply := make(chan Result)
		// 0..5 fingers: 3 odd and 3 even values, so the game is fair (50/50)
		pick := Msg{value: rand.IntN(6), playerID: id, reply: reply}

		if !send(out, pick, done) {
			return
		}
		select {
		case res := <-reply:
			if res != Won {
				return // Lost or Champion
			}
		case <-done:
			return
		}
	}
}

// addPlayer starts a player and returns the channel on which it sends its picks.
func addPlayer(id string, done <-chan struct{}) <-chan Msg {
	ch := make(chan Msg)
	go Player(ch, id, done)
	return ch
}
