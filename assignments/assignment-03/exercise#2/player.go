package main

import (
	"fmt"
	"math/rand/v2"
)

// Msg is the message sent by a player to the match it is playing.
type Msg struct {
	value    int       // number of fingers shown: 0..5
	playerID string
	reply    chan bool // where the match sends the outcome (true = won)
}

// Player is a client process. Its path is the list of request channels of the
// matches it would play, one per round, if it keeps winning.
func Player(id int, path []chan<- Msg) {
	name := fmt.Sprintf("player-%d", id)
	reply := make(chan bool)
	for _, request := range path {
		request <- Msg{value: rand.IntN(6), playerID: name, reply: reply}
		if won := <-reply; !won {
			return
		}
	}
}
