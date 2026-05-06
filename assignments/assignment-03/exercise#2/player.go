package main

import (
	"math/rand/v2"
)

type Msg struct {
	value    int
	playerId string
	reply    chan bool
}

func Player(ch chan Msg, id string, done <-chan struct{}) {
	for {
		replyCh := make(chan bool)
		pick := rand.IntN(5) + 1

		// send pick — abort if tournament is over
		select {
		case ch <- Msg{value: pick, playerId: id, reply: replyCh}:
		case <-done:
			return
		}

		// wait for the match result — abort if tournament is over
		select {
		case won := <-replyCh:
			if !won {
				return // eliminated, stop playing
			}
			// won this round: loop and send a new pick for the next match
		case <-done:
			return
		}
	}
}

func addPlayer(id string, done <-chan struct{}) chan Msg {
	ch := make(chan Msg)
	go Player(ch, id, done)
	return ch
}
