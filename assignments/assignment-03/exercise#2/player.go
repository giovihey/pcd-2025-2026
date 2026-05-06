package main

import (
	"fmt"
	"math/rand/v2"
)

type Msg struct {
	value    int
	playerId string
	reply    chan bool
}

func Player(ch chan Msg, id string, done chan struct{}) {
	replyCh := make(chan bool)
	pick := rand.IntN(5) + 1
	fmt.Printf("%s picks %d\n", id, pick)
	ch <- Msg{value: pick, playerId: id, reply: replyCh}

	won := <-replyCh
	if won {
		fmt.Printf("%s won!\n", id)
	} else {
		fmt.Printf("%s lost.\n", id)
	}
	done <- struct{}{}
}

func addPlayer(id string) (chan Msg, chan struct{}) {
	ch := make(chan Msg)
	done := make(chan struct{})
	go Player(ch, id, done)
	return ch, done
}
