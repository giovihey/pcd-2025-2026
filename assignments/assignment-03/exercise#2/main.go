package main

import (
	"fmt"
)

func main() {
	matchRounds := 4
	numPlayers := 1 << matchRounds
	done := make(chan struct{})

	var channels []chan Msg
	for i := 0; i < numPlayers; i++ {
		ch := addPlayer(fmt.Sprintf("player-%d", i), done)
		channels = append(channels, ch)
	}
	finalCh := spawnRound(channels, done)

	// the last message on finalCh is the overall winner
	winner := <-finalCh
	fmt.Printf("Tournament winner: %s\n", winner.playerId)

	// signal all goroutines (players + match forwarders) to stop
	close(done)
}
